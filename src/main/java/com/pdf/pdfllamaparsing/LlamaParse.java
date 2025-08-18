package com.pdf.pdfllamaparsing;
import okhttp3.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class LlamaParse {
    private static final String API_KEY = "llx-oA999tsrMARvnbHx1VByGTmGeAM0dV21BqmQhD8wUjDe790o"; // See how to get your API key at https://docs.cloud.llamaindex.ai/api_key
    private static final String UPLOAD_URL = "https://api.cloud.llamaindex.ai/api/v1/parsing/upload"; //upload the file to llamaIndex
    private static final String BASE_URL = "https://api.cloud.llamaindex.ai/api/v1/parsing/job/"; //ParsingJob

    private final OkHttpClient client;
    private final ObjectMapper objectMapper;

    public LlamaParse() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(300, TimeUnit.SECONDS)
                .readTimeout(300, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public static void main(String[] args) {
        LlamaParse parser = new LlamaParse();
        System.out.println("LlamaParse started...");
        try {
            String filePath = "src/main/resources/PurchaseInvoice.pdf";

            // Check if file exists
            File pdfFile = new File(filePath);
            if (!pdfFile.exists() || !pdfFile.isFile()) {
                System.err.println("Error: File not found at " + pdfFile.getAbsolutePath());
                return;
            }
            System.out.println("File found at " + pdfFile.getAbsolutePath());
            String jobId = parser.uploadFile(filePath);
            System.out.println("File uploaded successfully. Job ID: " + jobId);

            String result = parser.pollForResult(jobId);
            System.out.println("Parsing completed!");
            System.out.println("Markdown result: " + result);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String uploadFile(String filePath) throws IOException {
        File file = new File(filePath);

        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.getName(),
                        RequestBody.create(file, MediaType.parse("application/pdf")))

        // The parsing mode
        .addFormDataPart("parse_mode", "parse_page_with_agent")
                // The model to use
                .addFormDataPart("model", "anthropic-sonnet-3.5") //convert the model here
                // Whether to use high resolution OCR (Slow)
                .addFormDataPart("high_res_ocr", "true")
                // Adaptive long table. LlamaParse will try to detect long table and adapt the output
                .addFormDataPart("adaptive_long_table", "true")
                // Whether to try to extract outlined tables
                .addFormDataPart("outlined_table_extraction", "true")
                // Whether to output tables as HTML in the markdown output
                .addFormDataPart("output_tables_as_HTML", "true")
                // The page separator
                .addFormDataPart("page_separator", "\n\n---\n\n");

        RequestBody requestBody = builder.build();

        Request request = new Request.Builder()
                .url(UPLOAD_URL)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Upload failed: " + response.body().string());
            }

            String responseBody = response.body().string();
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            return jsonNode.get("id").asText();
        }
    }

    private String pollForResult(String jobId) throws IOException, InterruptedException {
        String resultUrl = BASE_URL + jobId + "/result/markdown"; //if need to convert to json use - result/json

        while (true) {
            Thread.sleep(5000); // Wait 5 seconds

            Request request = new Request.Builder()
                    .url(resultUrl)
                    .addHeader("Authorization", "Bearer " + API_KEY)
                    .get()
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    JsonNode jsonNode = objectMapper.readTree(responseBody);
                    return jsonNode.get("markdown").asText(); //only return responsebody if you want to use json
                } else if (response.code() == 400) {
                    String responseBody = response.body().string();
                    JsonNode jsonNode = objectMapper.readTree(responseBody);
                    String detail = jsonNode.get("detail").asText();

                    if ("Job not completed yet".equals(detail)) {
                        System.out.println("Job still processing...");
                        continue;
                    } else {
                        throw new IOException("Error: " + responseBody);
                    }
                } else {
                    throw new IOException("Error checking job status: " + response.body().string());
                }
            }
        }
    }
}
