



package com.example.infra;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ec2.VpcProps;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.ecs.ContainerImage;
import software.amazon.awscdk.services.ecs.Secret;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedTaskImageOptions;
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.secretsmanager.SecretStringGenerator;
import software.constructs.Construct;

import java.util.List;
import java.util.Map;

public class CustomerGraphqlStack extends Stack {

    public CustomerGraphqlStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        // Required: set in cdk.json context or pass at deploy time:
        //   cdk deploy --context imageUri=123456789012.dkr.ecr.us-east-1.amazonaws.com/my-repo:latest
        String imageUri = (String) this.getNode().tryGetContext("imageUri");
        if (imageUri == null || imageUri.isBlank()) {
            throw new RuntimeException(
                "CDK context 'imageUri' is required.\n" +
                "Pass it with: cdk deploy --context imageUri=<account>.dkr.ecr.<region>.amazonaws.com/<repo>:<tag>\n" +
                "Or add it under 'context' in cdk.json."
            );
        }

        Vpc vpc = new Vpc(this, "Vpc", VpcProps.builder()
                .maxAzs(2)
                .build());

        Cluster cluster = Cluster.Builder.create(this, "Cluster")
                .vpc(vpc)
                .build();

        // Secret holds JWT issuer + JWKS URIs. Populate before first deploy:
        //   aws secretsmanager put-secret-value \
        //     --secret-id customer-graphql/jwt-config \
        //     --secret-string '{"jwtIssuerUri":"https://...","jwtJwkSetUri":"https://.../.well-known/jwks.json"}'
        software.amazon.awscdk.services.secretsmanager.Secret jwtSecret =
                software.amazon.awscdk.services.secretsmanager.Secret.Builder.create(this, "JwtSecret")
                        .secretName("customer-graphql/jwt-config")
                        .description("JWT issuer and JWKS URIs for Customer GraphQL API")
                        .generateSecretString(SecretStringGenerator.builder()
                                .secretStringTemplate("{\"jwtIssuerUri\":\"REPLACE_ME\",\"jwtJwkSetUri\":\"REPLACE_ME\"}")
                                .generateStringKey("_unused")
                                .build())
                        .build();

        Map<String, Secret> containerSecrets = Map.of(
                "SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI",
                Secret.fromSecretsManager(jwtSecret, "jwtIssuerUri"),
                "SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI",
                Secret.fromSecretsManager(jwtSecret, "jwtJwkSetUri")
        );

        ApplicationLoadBalancedFargateService fargateService =
                ApplicationLoadBalancedFargateService.Builder.create(this, "FargateService")
                        .cluster(cluster)
                        .cpu(256)
                        .memoryLimitMiB(512)
                        .desiredCount(1)
                        .publicLoadBalancer(true)
                        .taskImageOptions(ApplicationLoadBalancedTaskImageOptions.builder()
                                .image(ContainerImage.fromRegistry(imageUri))
                                .containerPort(8080)
                                .environment(Map.of("SPRING_PROFILES_ACTIVE", "prod"))
                                .secrets(containerSecrets)
                                .build())
                        .build();

        jwtSecret.grantRead(fargateService.getTaskDefinition().getTaskRole());

        // ecr:GetAuthorizationToken must be on * (AWS API requirement — not resource-scoped).
        fargateService.getTaskDefinition().getExecutionRole().addToPrincipalPolicy(
                PolicyStatement.Builder.create()
                        .effect(Effect.ALLOW)
                        .actions(List.of("ecr:GetAuthorizationToken"))
                        .resources(List.of("*"))
                        .build()
        );

        // Pull permissions scoped to the specific cross-account repository.
        fargateService.getTaskDefinition().getExecutionRole().addToPrincipalPolicy(
                PolicyStatement.Builder.create()
                        .effect(Effect.ALLOW)
                        .actions(List.of(
                                "ecr:BatchCheckLayerAvailability",
                                "ecr:GetDownloadUrlForLayer",
                                "ecr:BatchGetImage"
                        ))
                        .resources(List.of(repoArnFromEcrUri(imageUri)))
                        .build()
        );

        fargateService.getTargetGroup().configureHealthCheck(HealthCheck.builder()
                .path("/actuator/health")
                .healthyHttpCodes("200")
                .interval(Duration.seconds(30))
                .timeout(Duration.seconds(5))
                .healthyThresholdCount(2)
                .unhealthyThresholdCount(3)
                .build());

        CfnOutput.Builder.create(this, "LoadBalancerDns")
                .description("ALB DNS — access the GraphQL API at http://<dns>/graphql")
                .value(fargateService.getLoadBalancer().getLoadBalancerDnsName())
                .build();
    }

    /**
     * Derives the ECR repository ARN from a full image URI.
     * e.g. 123456789012.dkr.ecr.us-east-1.amazonaws.com/my-repo:latest
     *   -> arn:aws:ecr:us-east-1:123456789012:repository/my-repo
     */
    private static String repoArnFromEcrUri(String imageUri) {
        String host = imageUri.split("/")[0];
        String repoAndTag = imageUri.substring(host.length() + 1);
        String repoName = repoAndTag.contains(":") ? repoAndTag.split(":")[0] : repoAndTag;
        String accountId = host.split("\\.")[0];
        String region = host
                .replaceFirst("^[^.]+\\.dkr\\.ecr\\.", "")
                .replace(".amazonaws.com", "");
        return String.format("arn:aws:ecr:%s:%s:repository/%s", region, accountId, repoName);
    }
}