package com.example.inventory.sync;

import java.util.UUID;

public record ConflictItemResponse(
    UUID productId, String productName, int quantityRequested, int shortfall) {}
