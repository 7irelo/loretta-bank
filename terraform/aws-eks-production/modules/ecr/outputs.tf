output "repository_urls" {
  description = "Map of repository name to repository URL."
  value       = { for k, v in aws_ecr_repository.this : k => v.repository_url }
}

output "repository_arns" {
  description = "Map of repository name to repository ARN."
  value       = { for k, v in aws_ecr_repository.this : k => v.arn }
}

output "registry_id" {
  description = "The registry ID where the repositories were created."
  value       = values(aws_ecr_repository.this)[0].registry_id
}
