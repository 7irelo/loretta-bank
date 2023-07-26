provider "aws" {
  region = "us-east-1"
}

resource "aws_eks_cluster" "loretta_bank_cluster" {
  name     = "loretta-bank-cluster"
  role_arn = var.cluster_role_arn
  vpc_config {
    subnet_ids = var.subnet_ids
  }
}
