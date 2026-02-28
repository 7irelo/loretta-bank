output "alb_controller_iam_role_arn" {
  description = "ARN of the IRSA role for the AWS Load Balancer Controller."
  value       = aws_iam_role.alb_controller.arn
}

output "alb_controller_iam_policy_arn" {
  description = "ARN of the IAM policy for the AWS Load Balancer Controller."
  value       = aws_iam_policy.alb_controller.arn
}

output "helm_release_name" {
  description = "Name of the Helm release."
  value       = helm_release.alb_controller.name
}

output "helm_release_status" {
  description = "Status of the Helm release."
  value       = helm_release.alb_controller.status
}
