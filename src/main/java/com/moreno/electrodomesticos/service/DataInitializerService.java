package com.moreno.electrodomesticos.service;

import com.moreno.electrodomesticos.model.Electrodomestico;
import com.moreno.electrodomesticos.repository.ElectrodomesticoRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Pobla la BD con datos de muestra si está vacía al arrancar.
 */
@Service
public class DataInitializerService {

    private final ElectrodomesticoRepository repository;

    public DataInitializerService(ElectrodomesticoRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    public void init() {
        if (repository.count() > 0) return;

        repository.saveAll(List.of(
            build("Frigorífico", "Samsung", "RB38T603DB1", new BigDecimal("899.99"), "A+++", "595×595×2010mm", "Total No Frost, 385L, Dispensador agua"),
            build("Frigorífico", "LG", "GBB72PZVGN", new BigDecimal("749.00"), "A++", "595×595×2030mm", "LinearCooling, 384L, DoorCooling+"),
            build("Frigorífico", "Bosch", "KGN86AIDR", new BigDecimal("1099.00"), "A+++", "595×705×2010mm", "VitaFresh Pro, 631L, No Frost"),
            build("Lavadora", "Samsung", "WW90T684DLH", new BigDecimal("649.00"), "A", "600×550×850mm", "9kg, 1400rpm, AI Control"),
            build("Lavadora", "LG", "F4WV509S1E", new BigDecimal("599.00"), "A+++", "600×560×850mm", "9kg, 1400rpm, AI DD™"),
            build("Lavadora", "Bosch", "WAN28282ES", new BigDecimal("549.00"), "A+++", "600×550×840mm", "8kg, 1400rpm, EcoSilence"),
            build("Lavavajillas", "Siemens", "SN23HW64CE", new BigDecimal("699.00"), "A+++", "598×550×815mm", "13 servicios, WiFi, varioFlex"),
            build("Lavavajillas", "Bosch", "SMV4HTX31E", new BigDecimal("749.00"), "A+++", "598×550×815mm", "14 servicios, PerfectDry, InfoLight"),
            build("Horno", "Balay", "3HB4331X0", new BigDecimal("329.00"), "A", "594×548×576mm", "71L, Multifunción, Inox"),
            build("Horno", "Siemens", "HB578A0S0", new BigDecimal("549.00"), "A+", "594×548×576mm", "71L, Vapor, iQ500"),
            build("Microondas", "LG", "MS2336DB", new BigDecimal("149.00"), "B", "489×275×382mm", "23L, 800W, Negro"),
            build("Microondas", "Samsung", "MS23K3513AK", new BigDecimal("129.00"), "B", "489×281×275mm", "23L, 800W, Eco Mode"),
            build("Secadora", "Samsung", "DV90TA240AE", new BigDecimal("699.00"), "A++", "600×600×850mm", "9kg, Bomba de calor, Wi-Fi"),
            build("Secadora", "LG", "RC90V9AV2Q", new BigDecimal("749.00"), "A+++", "600×560×850mm", "9kg, Bomba de calor, ThinQ"),
            build("Aire Acondicionado", "Daikin", "FTXC35C", new BigDecimal("799.00"), "A++", "750×260×270mm", "3500W, R-32, Wifi opcional")
        ));
    }

    private Electrodomestico build(String tipo, String marca, String modelo,
                                   BigDecimal precio, String clase, String dims, String specs) {
        return Electrodomestico.builder()
                .tipo(tipo).marca(marca).modelo(modelo)
                .precio(precio).clasificacionEnergetica(clase)
                .dimensiones(dims).especificacionesPrincipales(specs)
                .build();
    }
}
