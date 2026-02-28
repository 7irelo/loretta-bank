output "jwt_secret_arn" {
  description = "ARN of the JWT signing secret."
  value       = aws_secretsmanager_secret.jwt.arn
}

output "bootstrap_credentials_secret_arn" {
  description = "ARN of the bootstrap credentials secret."
  value       = aws_secretsmanager_secret.bootstrap_credentials.arn
}

output "eso_iam_role_arn" {
  description = "ARN of the IRSA role for External Secrets Operator."
  value       = aws_iam_role.eso.arn
}

output "eso_iam_policy_arn" {
  description = "ARN of the IAM policy for External Secrets Operator."
  value       = aws_iam_policy.eso.arn
}
