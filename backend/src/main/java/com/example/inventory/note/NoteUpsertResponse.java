package com.example.inventory.note;

public record NoteUpsertResponse(NoteResponse note, boolean conflicted) {}
