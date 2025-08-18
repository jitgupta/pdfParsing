1. Setup and Configuration
•	API Connection Details:
o	Uses an API key for authentication with LlamaIndex
o	Defines endpoints for file upload and job status checking

•	HTTP Client Configuration:
o	Creates an OkHttpClient with extended timeouts (300 seconds)
o	Configures Jackson ObjectMapper for JSON processing
2. PDF Upload Process
•	File Validation:
o	Checks if the specified PDF file exists at the provided path
o	Reports errors if the file is not found
•	Multipart Request Creation:
o	Builds a multipart form request with the PDF file
o	Configures parsing options including: 
	parse_mode: "parse_page_with_agent"
	model: "anthropic-sonnet-3.5"
	high_res_ocr: "true" (for better text recognition)
	adaptive_long_table: "true" (for handling large tables)
	outlined_table_extraction: "true"
	output_tables_as_HTML: "true"
	page_separator: "\n\n---\n\n" (to separate pages in output)
•	Upload Execution:
o	Sends the request with appropriate authorization header
o	Extracts the job ID from the successful response
3. Result Polling Mechanism
•	Asynchronous Result Retrieval:
o	Repeatedly checks job status every 5 seconds
o	Requests the markdown format of the parsed result
•	Response Handling:
o	Processes successful responses by extracting the markdown content
o	Handles 400 status codes with "Job not completed yet" messages
o	Throws exceptions for other error cases
4. Main Execution Flow
1.	Creates a LlamaParse instance
2.	Validates the target PDF file exists
3.	Uploads the file to LlamaIndex's parsing service
4.	Polls for the parsing result until completion
5.	Displays the markdown result when ready
This implementation provides a complete workflow for transforming PDF documents into structured markdown text using LlamaIndex's AI-powered parsing capabilities, with particular attention to table extraction and high-resolution OCR.
