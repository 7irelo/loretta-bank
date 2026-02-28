output "bootstrap_brokers" {
  description = "Plaintext bootstrap broker connection string."
  value       = aws_msk_cluster.this.bootstrap_brokers
}

output "bootstrap_brokers_tls" {
  description = "TLS bootstrap broker connection string."
  value       = aws_msk_cluster.this.bootstrap_brokers_tls
}

output "cluster_arn" {
  description = "ARN of the MSK cluster."
  value       = aws_msk_cluster.this.arn
}

output "zookeeper_connect_string" {
  description = "ZooKeeper connection string (empty for KRaft mode)."
  value       = aws_msk_cluster.this.zookeeper_connect_string
}

output "msk_security_group_id" {
  description = "Security group ID for the MSK cluster."
  value       = aws_security_group.msk.id
}
