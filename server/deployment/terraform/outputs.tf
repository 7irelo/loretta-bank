output "cluster_endpoint" {
  value = aws_eks_cluster.loretta_bank_cluster.endpoint
}

output "cluster_arn" {
  value = aws_eks_cluster.loretta_bank_cluster.arn
}
