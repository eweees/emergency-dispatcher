package com.emergencydispatcher.bridge;

import javafx.application.Platform;

import java.util.function.Consumer;

/**
 * Мост между JavaScript (Leaflet-карта в WebView) и Java-кодом.
 * Экземпляр регистрируется в JS как window.mapBridge через JSObject.setMember().
 * Все public-методы доступны из JavaScript.
 */
public class MapBridge {

    private double selectedLat;
    private double selectedLng;
    private String selectedAddress = "";

    private Consumer<String> onAddressSelected;
    private Runnable onGpsRequested;

    // Вызывается из JavaScript при выборе точки на карте
    public void onLocationSelected(double lat, double lng, String address) {
        this.selectedLat = lat;
        this.selectedLng = lng;
        this.selectedAddress = address;
        Platform.runLater(() -> {
            if (onAddressSelected != null) onAddressSelected.accept(address);
        });
    }

    // Вызывается из JavaScript если браузерный Geolocation не сработал
    public void requestGpsLocation() {
        Platform.runLater(() -> {
            if (onGpsRequested != null) onGpsRequested.run();
        });
    }

    public double getSelectedLat() { return selectedLat; }
    public double getSelectedLng() { return selectedLng; }
    public String getSelectedAddress() { return selectedAddress; }
    public boolean hasLocation() { return !selectedAddress.isBlank(); }

    public void setOnAddressSelected(Consumer<String> handler) { this.onAddressSelected = handler; }
    public void setOnGpsRequested(Runnable handler) { this.onGpsRequested = handler; }
}
