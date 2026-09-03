package com.example.inventory.sync;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ResolveConflictRequest(
    @NotEmpty List<@Valid ConflictAdjustment> adjustments, @NotBlank String note) {}
