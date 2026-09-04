package com.example.inventory.product;

import com.example.inventory.exception.BadRequestException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Stores product photos as files on local disk (Phase 14's chosen approach - no external object
 * storage dependency). One file per product, named by product id, so a re-upload simply overwrites
 * the previous photo and there's nothing to garbage-collect.
 */
@Service
public class ProductImageStorageService {

  private static final Map<String, String> EXTENSION_BY_CONTENT_TYPE =
      Map.of(
          "image/jpeg", "jpg",
          "image/png", "png",
          "image/webp", "webp");

  private static final Set<String> ALLOWED_CONTENT_TYPES = EXTENSION_BY_CONTENT_TYPE.keySet();

  private final Path storageDir;

  public ProductImageStorageService(@Value("${app.storage.product-images-dir}") String storageDir) {
    this.storageDir = Path.of(storageDir).toAbsolutePath().normalize();
    try {
      Files.createDirectories(this.storageDir);
    } catch (IOException e) {
      throw new UncheckedIOException("Could not create product image storage directory", e);
    }
  }

  /**
   * Stores the upload, replacing any existing image for this product, and returns its extension.
   */
  public String store(UUID productId, MultipartFile file) {
    String contentType = file.getContentType();
    if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
      throw new BadRequestException(
          "Unsupported image type - only JPEG, PNG, and WebP are accepted");
    }
    if (file.isEmpty()) {
      throw new BadRequestException("The uploaded file is empty");
    }

    String extension = EXTENSION_BY_CONTENT_TYPE.get(contentType);
    deleteExistingRegardlessOfExtension(productId);
    Path target = pathFor(productId, extension);
    try {
      file.transferTo(target);
    } catch (IOException e) {
      throw new UncheckedIOException("Could not store product image", e);
    }
    return extension;
  }

  /** Returns the stored file for a product, if one exists. */
  public java.util.Optional<Path> find(UUID productId) {
    for (String extension : EXTENSION_BY_CONTENT_TYPE.values()) {
      Path candidate = pathFor(productId, extension);
      if (Files.exists(candidate)) {
        return java.util.Optional.of(candidate);
      }
    }
    return java.util.Optional.empty();
  }

  public void delete(UUID productId) {
    deleteExistingRegardlessOfExtension(productId);
  }

  public String contentTypeFor(Path path) {
    String fileName = path.getFileName().toString();
    String extension = fileName.substring(fileName.lastIndexOf('.') + 1);
    return EXTENSION_BY_CONTENT_TYPE.entrySet().stream()
        .filter(entry -> entry.getValue().equals(extension))
        .map(Map.Entry::getKey)
        .findFirst()
        .orElse("application/octet-stream");
  }

  private void deleteExistingRegardlessOfExtension(UUID productId) {
    find(productId)
        .ifPresent(
            existing -> {
              try {
                Files.deleteIfExists(existing);
              } catch (IOException e) {
                throw new UncheckedIOException("Could not delete existing product image", e);
              }
            });
  }

  private Path pathFor(UUID productId, String extension) {
    // productId is a server-generated UUID, never user input, so this can't be a path traversal
    // vector - but resolving under storageDir and normalizing keeps that invariant enforced even
    // if that ever changes.
    return storageDir.resolve(productId + "." + extension).normalize();
  }
}
