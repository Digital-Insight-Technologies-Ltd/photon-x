ROLE_ARN="arn:aws:iam::891813249681:role/OrganizationAccountAccessRole"
CREDS=$(aws sts assume-role --role-arn $ROLE_ARN --role-session-name LocalDockerBuild)

export AWS_ACCESS_KEY_ID=$(echo $CREDS | jq -r .Credentials.AccessKeyId)
export AWS_SECRET_ACCESS_KEY=$(echo $CREDS | jq -r .Credentials.SecretAccessKey)
export AWS_SESSION_TOKEN=$(echo $CREDS | jq -r .Credentials.SessionToken)

aws ecr get-login-password --region us-west-2 | docker login --username AWS --password-stdin 891813249681.dkr.ecr.us-west-2.amazonaws.com
docker build . -t 891813249681.dkr.ecr.us-west-2.amazonaws.com/photon-api:latest
docker push 891813249681.dkr.ecr.us-west-2.amazonaws.com/photon-api:latest