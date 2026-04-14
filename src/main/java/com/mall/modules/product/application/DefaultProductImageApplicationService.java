package com.mall.modules.product.application;

import com.mall.common.api.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.config.MinioProperties;
import com.mall.modules.product.vo.ProductImageUploadVO;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.SetBucketPolicyArgs;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

/**
 * 商品图片应用服务，负责把上传文件写入 MinIO 并返回可访问地址。
 */
@Service
@Transactional(readOnly = true)
public class DefaultProductImageApplicationService implements ProductImageApplicationService {

	/** 公开读 bucket 策略模板。 */
	private static final String PUBLIC_READ_BUCKET_POLICY_TEMPLATE = """
		{
		  "Version":"2012-10-17",
		  "Statement":[
		    {
		      "Effect":"Allow",
		      "Principal":{"AWS":["*"]},
		      "Action":["s3:GetObject"],
		      "Resource":["arn:aws:s3:::%s/*"]
		    }
		  ]
		}
		""";

	private final MinioClient minioClient;
	private final MinioProperties minioProperties;

	public DefaultProductImageApplicationService(MinioClient minioClient, MinioProperties minioProperties) {
		this.minioClient = minioClient;
		this.minioProperties = minioProperties;
	}

	/**
	 * 上传商品图片到 MinIO，并返回对象 key 和访问地址。
	 */
	@Override
	public ProductImageUploadVO uploadImage(MultipartFile file) {
		validateImageFile(file);
		ensureBucketReady();

		String objectKey = buildObjectKey(file.getOriginalFilename());

		try (InputStream inputStream = file.getInputStream()) {
			minioClient.putObject(
				PutObjectArgs.builder()
					.bucket(minioProperties.getBucket())
					.object(objectKey)
					.stream(inputStream, file.getSize(), -1)
					.contentType(file.getContentType())
					.build()
			);
		} catch (IOException exception) {
			throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Failed to read uploaded image");
		} catch (Exception exception) {
			throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Failed to upload image to MinIO");
		}

		return new ProductImageUploadVO(objectKey, buildImageUrl(objectKey));
	}

	/**
	 * 校验上传文件是否为有效图片。
	 */
	private void validateImageFile(MultipartFile file) {
		// 先做最基础的文件校验，避免把空文件或非图片内容写进对象存储。
		if (file == null || file.isEmpty()) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, "Image file must not be empty");
		}

		String contentType = file.getContentType();
		if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, "Only image files are supported");
		}
	}

	/**
	 * 确保图片 bucket 已存在且具备公开读策略。
	 */
	private void ensureBucketReady() {
		try {
			boolean bucketExists = minioClient.bucketExists(
				BucketExistsArgs.builder().bucket(minioProperties.getBucket()).build()
			);

			if (!bucketExists) {
				// 本地环境下如果 bucket 还没建好，这里自动补建，省得每次手动处理。
				minioClient.makeBucket(MakeBucketArgs.builder().bucket(minioProperties.getBucket()).build());
			}

			// 练手项目默认把商品图 bucket 配成公开读，这样前端能直接用返回地址展示图片。
			minioClient.setBucketPolicy(
				SetBucketPolicyArgs.builder()
					.bucket(minioProperties.getBucket())
					.config(PUBLIC_READ_BUCKET_POLICY_TEMPLATE.formatted(minioProperties.getBucket()))
					.build()
			);
		} catch (BusinessException exception) {
			throw exception;
		} catch (Exception exception) {
			throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Failed to prepare MinIO bucket");
		}
	}

	/**
	 * 生成对象存储里的文件路径。
	 */
	private String buildObjectKey(String originalFilename) {
		LocalDate today = LocalDate.now();
		String extension = resolveExtension(originalFilename);

		// 按日期分目录，后面 bucket 里文件多了也更容易排查。
		return "product-images/%d/%02d/%02d/%s%s".formatted(
			today.getYear(),
			today.getMonthValue(),
			today.getDayOfMonth(),
			UUID.randomUUID().toString().replace("-", ""),
			extension
		);
	}

	/**
	 * 提取原始文件扩展名，缺失时回退为二进制扩展名。
	 */
	private String resolveExtension(String originalFilename) {
		if (originalFilename == null || !originalFilename.contains(".")) {
			return ".bin";
		}

		return originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase(Locale.ROOT);
	}

	/**
	 * 拼装图片的最终访问地址。
	 */
	private String buildImageUrl(String objectKey) {
		String publicBaseUrl = trimTrailingSlash(minioProperties.getPublicBaseUrl());
		return publicBaseUrl + "/" + minioProperties.getBucket() + "/" + objectKey;
	}

	/**
	 * 去掉地址末尾的斜杠，避免拼接 URL 时出现双斜杠。
	 */
	private String trimTrailingSlash(String value) {
		if (value == null || value.isBlank()) {
			return trimTrailingSlash(minioProperties.getEndpoint());
		}

		return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
	}
}
