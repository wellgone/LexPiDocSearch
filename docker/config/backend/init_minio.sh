#!/bin/sh

# 等待MinIO启动
until mc alias set myminio http://minio:9000 "${MINIO_ROOT_USER}" "${MINIO_ROOT_PASSWORD}"; do
    echo 'Waiting for MinIO to be ready...'
    sleep 1
done

# 创建bucket
mc mb myminio/lpms --ignore-existing

# 设置bucket策略为public
mc policy set public myminio/lpms

# 创建新的access key和secret key
mc admin user add myminio "${MINIO_ACCESS_KEY}" "${MINIO_SECRET_KEY}"

# 创建只读策略
echo '{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "s3:GetObject",
                "s3:ListBucket"
            ],
            "Resource": [
                "arn:aws:s3:::lpms/*",
                "arn:aws:s3:::lpms"
            ]
        }
    ]
}' > /tmp/read-only.json

# 创建读写策略
echo '{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "s3:*"
            ],
            "Resource": [
                "arn:aws:s3:::lpms/*",
                "arn:aws:s3:::lpms"
            ]
        }
    ]
}' > /tmp/readwrite.json

# 创建策略
mc admin policy create myminio read-only /tmp/read-only.json
mc admin policy create myminio readwrite /tmp/readwrite.json

# 将策略附加到用户
mc admin policy attach myminio readwrite --user "${MINIO_ACCESS_KEY}"

echo "MinIO initialization completed successfully" 