package com.intellidesk.cognitia.ingestion.service.preprocessingStrategy;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.ai.document.Document;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TabularPreprocessingStrategy implements PreprocessingStrategy {

    private static final int ROWS_PER_CHUNK = 50;

    @Override
    public List<Document> preprocess(Resource resource,
            com.intellidesk.cognitia.ingestion.models.entities.Resource rawSource) {

        String format = normalizeFormat(rawSource.getFormat());
        try {
            if ("csv".equals(format)) {
                return processCsv(resource, rawSource);
            } else {
                return processSpreadsheet(resource, rawSource);
            }
        } catch (Exception e) {
            log.error("Tabular preprocessing failed for resource {}: {}", rawSource.getResId(), e.getMessage(), e);
            throw new RuntimeException("Failed to preprocess tabular file: " + e.getMessage(), e);
        }
    }

    private List<Document> processCsv(Resource resource,
            com.intellidesk.cognitia.ingestion.models.entities.Resource rawSource)
            throws IOException, CsvException {

        List<String[]> allRows;
        try (InputStream is = resource.getInputStream();
             CSVReader reader = new CSVReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            allRows = reader.readAll();
        }

        if (allRows.isEmpty()) {
            return List.of();
        }

        String[] headers = allRows.getFirst();
        List<String[]> dataRows = allRows.subList(1, allRows.size());

        List<Document> documents = new ArrayList<>();

        documents.add(createSchemaDocument(headers, dataRows.size(), null, rawSource));

        for (int i = 0; i < dataRows.size(); i += ROWS_PER_CHUNK) {
            int end = Math.min(i + ROWS_PER_CHUNK, dataRows.size());
            List<String[]> chunk = dataRows.subList(i, end);
            documents.add(createRowChunkDocument(headers, chunk, i + 1, end, null, rawSource));
        }

        return documents;
    }

    private List<Document> processSpreadsheet(Resource resource,
            com.intellidesk.cognitia.ingestion.models.entities.Resource rawSource) throws IOException {

        List<Document> documents = new ArrayList<>();
        DataFormatter dataFormatter = new DataFormatter();

        try (InputStream is = resource.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {

            for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
                Sheet sheet = workbook.getSheetAt(s);
                String sheetName = sheet.getSheetName();

                if (sheet.getPhysicalNumberOfRows() == 0) continue;

                Row headerRow = sheet.getRow(sheet.getFirstRowNum());
                if (headerRow == null) continue;

                String[] headers = extractRowValues(headerRow, dataFormatter);
                int dataRowCount = sheet.getLastRowNum() - sheet.getFirstRowNum();

                documents.add(createSchemaDocument(headers, dataRowCount, sheetName, rawSource));

                List<String[]> rowBuffer = new ArrayList<>();
                int chunkStartRow = 1;

                for (int r = sheet.getFirstRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) continue;
                    rowBuffer.add(extractRowValues(row, dataFormatter));

                    if (rowBuffer.size() >= ROWS_PER_CHUNK) {
                        documents.add(createRowChunkDocument(headers, rowBuffer,
                                chunkStartRow, chunkStartRow + rowBuffer.size() - 1, sheetName, rawSource));
                        chunkStartRow += rowBuffer.size();
                        rowBuffer = new ArrayList<>();
                    }
                }

                if (!rowBuffer.isEmpty()) {
                    documents.add(createRowChunkDocument(headers, rowBuffer,
                            chunkStartRow, chunkStartRow + rowBuffer.size() - 1, sheetName, rawSource));
                }
            }
        }

        return documents;
    }

    private Document createSchemaDocument(String[] headers, int rowCount, String sheetName,
            com.intellidesk.cognitia.ingestion.models.entities.Resource rawSource) {

        StringBuilder sb = new StringBuilder();
        sb.append("Dataset Schema for: ").append(rawSource.getName()).append("\n");
        if (sheetName != null) {
            sb.append("Sheet: ").append(sheetName).append("\n");
        }
        sb.append("Total rows: ").append(rowCount).append("\n");
        sb.append("Columns (").append(headers.length).append("): ");
        sb.append(String.join(", ", headers)).append("\n");

        Map<String, Object> metadata = buildBaseMetadata(rawSource);
        metadata.put("chunkType", "schema");
        metadata.put("columnHeaders", String.join(",", headers));
        metadata.put("rowCount", rowCount);
        metadata.put("columnCount", headers.length);
        if (sheetName != null) metadata.put("sheetName", sheetName);

        return new Document(sb.toString(), metadata);
    }

    private Document createRowChunkDocument(String[] headers, List<String[]> rows,
            int startRow, int endRow, String sheetName,
            com.intellidesk.cognitia.ingestion.models.entities.Resource rawSource) {

        StringBuilder sb = new StringBuilder();
        sb.append(String.join(" | ", headers)).append("\n");
        sb.append("-".repeat(headers.length * 15)).append("\n");

        for (String[] row : rows) {
            List<String> paddedRow = new ArrayList<>();
            for (int c = 0; c < headers.length; c++) {
                paddedRow.add(c < row.length ? row[c] : "");
            }
            sb.append(String.join(" | ", paddedRow)).append("\n");
        }

        Map<String, Object> metadata = buildBaseMetadata(rawSource);
        metadata.put("chunkType", "rows");
        metadata.put("columnHeaders", String.join(",", headers));
        metadata.put("rowRange", startRow + "-" + endRow);
        if (sheetName != null) metadata.put("sheetName", sheetName);

        return new Document(sb.toString(), metadata);
    }

    private Map<String, Object> buildBaseMetadata(
            com.intellidesk.cognitia.ingestion.models.entities.Resource rawSource) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("tenantId", rawSource.getTenantId().toString());
        metadata.put("sourceId", rawSource.getResId().toString());
        metadata.put("sourceName", rawSource.getName());
        metadata.put("sourceUrl", rawSource.getUrl());
        metadata.put("sourceFormat", rawSource.getFormat());
        metadata.put("fileName", rawSource.getName());
        metadata.put("ingestionTimestamp", Instant.now().toString());
        metadata.put("contentType", resolveContentType(rawSource.getFormat()));
        return metadata;
    }

    private String[] extractRowValues(Row row, DataFormatter formatter) {
        int lastCell = row.getLastCellNum();
        String[] values = new String[lastCell >= 0 ? lastCell : 0];
        for (int c = 0; c < values.length; c++) {
            Cell cell = row.getCell(c, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            values[c] = cell.getCellType() == CellType.FORMULA
                    ? formatter.formatCellValue(cell)
                    : formatter.formatCellValue(cell);
        }
        return values;
    }

    private String normalizeFormat(String format) {
        if (format == null) return "";
        String trimmed = format.trim().toLowerCase();
        return trimmed.startsWith(".") ? trimmed.substring(1) : trimmed;
    }

    private String resolveContentType(String format) {
        String ext = normalizeFormat(format);
        return switch (ext) {
            case "csv" -> "text/csv";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "xls" -> "application/vnd.ms-excel";
            case "ods" -> "application/vnd.oasis.opendocument.spreadsheet";
            default -> "application/octet-stream";
        };
    }
}
