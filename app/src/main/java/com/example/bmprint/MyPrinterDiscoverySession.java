package com.example.bmprint;


import android.print.PrintAttributes;
import android.print.PrinterCapabilitiesInfo;
import android.print.PrinterId;
import android.print.PrinterInfo;
import android.printservice.PrintService;
import android.printservice.PrinterDiscoverySession;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class MyPrinterDiscoverySession extends PrinterDiscoverySession {
    private static final String TAG = "MyPrinterDiscoverySession";
    private final List<PrinterInfo> mPrinters = new ArrayList<>();
    private final PrintService mPrintService;

    public MyPrinterDiscoverySession(PrintService printService) {
        mPrintService = printService;
    }

    @Override
    public void onStartPrinterDiscovery(@NonNull List<PrinterId> priorityList) {
        Log.d(TAG, "Starting printer discovery");

        // Simulate discovery of a single printer
        PrinterId printerId = mPrintService.generatePrinterId("my_printer");
        PrinterCapabilitiesInfo capabilities = new PrinterCapabilitiesInfo.Builder(printerId).addMediaSize(PrintAttributes.MediaSize.ISO_A4, true).addResolution(new PrintAttributes.Resolution("default", "Default", 600, 600), true).setColorModes(PrintAttributes.COLOR_MODE_COLOR | PrintAttributes.COLOR_MODE_MONOCHROME, PrintAttributes.COLOR_MODE_COLOR).build();

        PrinterInfo printerInfo = new PrinterInfo.Builder(printerId, "BillPrinter", PrinterInfo.STATUS_IDLE).setCapabilities(capabilities).build();

        mPrinters.add(printerInfo);
        addPrinters(mPrinters);
    }

    @Override
    public void onStopPrinterDiscovery() {
        Log.d(TAG, "Stopping printer discovery");
    }

    @Override
    public void onValidatePrinters(@NonNull List<PrinterId> printerIds) {
        Log.d(TAG, "Validating printers");
        List<PrinterInfo> updatedPrinters = new ArrayList<>();
        for (PrinterId printerId : printerIds) {
            for (PrinterInfo printerInfo : mPrinters) {
                if (printerInfo.getId().equals(printerId)) {
                    updatedPrinters.add(printerInfo);
                    break;
                }
            }
        }
        addPrinters(updatedPrinters);
    }


    @Override
    public void onStartPrinterStateTracking(@NonNull PrinterId printerId) {
        Log.d(TAG, "Starting printer state tracking for " + printerId);
        for (PrinterInfo printerInfo : mPrinters) {
            if (printerInfo.getId().equals(printerId)) {
                addPrinters(mPrinters);
                break;
            }
        }
    }


    @Override
    public void onStopPrinterStateTracking(@NonNull PrinterId printerId) {
        Log.d(TAG, "Stopping printer state tracking for " + printerId);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Destroying printer discovery session");
    }
}