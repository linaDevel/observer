package ru.linachan.observer.service;

public interface Service extends Runnable {

    void onInit();
    void onShutDown();
}
