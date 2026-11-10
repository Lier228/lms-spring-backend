package kz.edu.lms.dto.file;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

public record FileResponse(
        @Schema(description = "Use this ID to download the file") UUID id,
        String filename,
        @Schema(description = "Size in bytes") long size
) {
}
