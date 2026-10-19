package tr.kitaptakip.dtos;

import java.util.List;

public record BookImportResponse(int totalRows, int importedRows, int failedRows, List<BookImportRowError> errors) { }
