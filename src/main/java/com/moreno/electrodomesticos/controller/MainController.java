package com.moreno.electrodomesticos.controller;

import com.moreno.electrodomesticos.model.Electrodomestico;
import com.moreno.electrodomesticos.service.ElectrodomesticoService;
import com.moreno.electrodomesticos.service.PdfService;
import com.moreno.electrodomesticos.util.AlertHelper;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.stage.FileChooser;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.util.*;

@Component
public class MainController implements Initializable {

    // ── Filters ──────────────────────────────────────────────────────────────
    @FXML private ComboBox<String> cbTipo;
    @FXML private ComboBox<String> cbMarca;
    @FXML private Button btnLimpiarFiltros;

    // ── Table ─────────────────────────────────────────────────────────────────
    @FXML private TableView<Electrodomestico> tablaElectrodomesticos;
    @FXML private TableColumn<Electrodomestico, Boolean>  colSeleccion;
    @FXML private TableColumn<Electrodomestico, String>   colTipo;
    @FXML private TableColumn<Electrodomestico, String>   colMarca;
    @FXML private TableColumn<Electrodomestico, String>   colModelo;
    @FXML private TableColumn<Electrodomestico, String>   colPrecio;
    @FXML private TableColumn<Electrodomestico, String>   colClase;
    @FXML private TableColumn<Electrodomestico, String>   colDimensiones;
    @FXML private TableColumn<Electrodomestico, String>   colSpecs;

    // ── Form ──────────────────────────────────────────────────────────────────
    @FXML private TextField txtTipo;
    @FXML private TextField txtMarca;
    @FXML private TextField txtModelo;
    @FXML private TextField txtPrecio;
    @FXML private TextField txtClase;
    @FXML private TextField txtDimensiones;
    @FXML private TextArea  txtSpecs;
    @FXML private Button    btnGuardar;
    @FXML private Button    btnNuevo;
    @FXML private Button    btnEliminar;
    @FXML private Button    btnImprimirSeleccionados;
    @FXML private Label     lblEstado;
    @FXML private Label     lblPaginaInfo;

    // ── State ─────────────────────────────────────────────────────────────────
    private final ElectrodomesticoService service;
    private final PdfService pdfService;

    private final ObservableList<Electrodomestico> items = FXCollections.observableArrayList();
    private final Map<Long, SimpleBooleanProperty> selectionMap = new HashMap<>();

    private Electrodomestico editando = null;
    private int currentPage = 0;
    private int totalPages  = 1;

    public MainController(ElectrodomesticoService service, PdfService pdfService) {
        this.service    = service;
        this.pdfService = pdfService;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Initializable
    // ═════════════════════════════════════════════════════════════════════════
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configurarColumnas();
        configurarFiltros();
        configurarScrollInfinito();
        cargarPagina(0, true);
    }

    // ── Column setup ──────────────────────────────────────────────────────────
    private void configurarColumnas() {
        colSeleccion.setCellValueFactory(cd -> {
            Long id = cd.getValue().getId();
            return selectionMap.computeIfAbsent(id, k -> new SimpleBooleanProperty(false));
        });
        colSeleccion.setCellFactory(CheckBoxTableCell.forTableColumn(colSeleccion));
        colSeleccion.setEditable(true);
        tablaElectrodomesticos.setEditable(true);

        colTipo.setCellValueFactory(cd       -> new SimpleStringProperty(cd.getValue().getTipo()));
        colMarca.setCellValueFactory(cd      -> new SimpleStringProperty(cd.getValue().getMarca()));
        colModelo.setCellValueFactory(cd     -> new SimpleStringProperty(cd.getValue().getModelo()));
        colPrecio.setCellValueFactory(cd     -> new SimpleStringProperty(
                cd.getValue().getPrecio() != null ? cd.getValue().getPrecio().toPlainString() + " €" : ""));
        colClase.setCellValueFactory(cd      -> new SimpleStringProperty(cd.getValue().getClasificacionEnergetica()));
        colDimensiones.setCellValueFactory(cd-> new SimpleStringProperty(cd.getValue().getDimensiones()));
        colSpecs.setCellValueFactory(cd      -> new SimpleStringProperty(cd.getValue().getEspecificacionesPrincipales()));

        // Click en fila → cargar en formulario
        tablaElectrodomesticos.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, selected) -> { if (selected != null) cargarEnFormulario(selected); });

        tablaElectrodomesticos.setItems(items);
    }

    // ── Filter setup ──────────────────────────────────────────────────────────
    private void configurarFiltros() {
        cbTipo.getItems().add("Todos");
        cbTipo.getItems().addAll(service.findTipos());
        cbTipo.setValue("Todos");

        cbMarca.getItems().add("Todas");
        cbMarca.getItems().addAll(service.findMarcas());
        cbMarca.setValue("Todas");

        cbTipo.valueProperty().addListener((obs, old, val) -> {
            String tipo = val != null && !val.equals("Todos") ? val : null;
            cbMarca.getItems().setAll("Todas");
            cbMarca.getItems().addAll(service.findMarcasByTipo(tipo));
            cbMarca.setValue("Todas");
            recargarDesdeInicio();
        });

        cbMarca.valueProperty().addListener((obs, old, val) -> recargarDesdeInicio());
    }

    // ── Infinite scroll ───────────────────────────────────────────────────────
    private void configurarScrollInfinito() {
        // Listener diferido: el ScrollBar se agrega al scene-graph después de layout
        tablaElectrodomesticos.skinProperty().addListener((obs, old, skin) -> {
            if (skin == null) return;
            Platform.runLater(() -> buscarScrollBar(tablaElectrodomesticos));
        });
    }

    private void buscarScrollBar(TableView<?> table) {
        for (Node node : table.lookupAll(".scroll-bar")) {
            if (node instanceof ScrollBar sb && sb.getOrientation() == Orientation.VERTICAL) {
                sb.valueProperty().addListener((obs, old, val) -> {
                    if (val.doubleValue() >= 0.95 && currentPage + 1 < totalPages) {
                        cargarPagina(currentPage + 1, false);
                    }
                });
                break;
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Data loading
    // ═════════════════════════════════════════════════════════════════════════
    private void recargarDesdeInicio() {
        selectionMap.clear();
        cargarPagina(0, true);
    }

    private void cargarPagina(int page, boolean resetear) {
        String tipo  = cbTipo.getValue();
        String marca = cbMarca.getValue();
        Page<Electrodomestico> resultado = service.findPaginated(page, tipo, marca);

        currentPage = resultado.getNumber();
        totalPages  = resultado.getTotalPages();

        if (resetear) {
            items.setAll(resultado.getContent());
        } else {
            items.addAll(resultado.getContent());
        }

        lblPaginaInfo.setText(String.format("Mostrando %d de %d registros",
                items.size(), resultado.getTotalElements()));
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Form helpers
    // ═════════════════════════════════════════════════════════════════════════
    private void cargarEnFormulario(Electrodomestico e) {
        editando = e;
        txtTipo.setText(e.getTipo());
        txtMarca.setText(e.getMarca());
        txtModelo.setText(e.getModelo());
        txtPrecio.setText(e.getPrecio() != null ? e.getPrecio().toPlainString() : "");
        txtClase.setText(e.getClasificacionEnergetica());
        txtDimensiones.setText(e.getDimensiones());
        txtSpecs.setText(e.getEspecificacionesPrincipales());
        btnGuardar.setText("Actualizar");
    }

    private void limpiarFormulario() {
        editando = null;
        txtTipo.clear(); txtMarca.clear(); txtModelo.clear();
        txtPrecio.clear(); txtClase.clear(); txtDimensiones.clear(); txtSpecs.clear();
        btnGuardar.setText("Guardar");
        tablaElectrodomesticos.getSelectionModel().clearSelection();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  FXML Actions
    // ═════════════════════════════════════════════════════════════════════════
    @FXML
    private void onNuevo() {
        limpiarFormulario();
    }

    @FXML
    private void onGuardar() {
        if (!validarFormulario()) return;

        Electrodomestico e = editando != null ? editando : new Electrodomestico();
        e.setTipo(txtTipo.getText().trim());
        e.setMarca(txtMarca.getText().trim());
        e.setModelo(txtModelo.getText().trim());
        e.setPrecio(new BigDecimal(txtPrecio.getText().trim().replace(",", ".")));
        e.setClasificacionEnergetica(txtClase.getText().trim());
        e.setDimensiones(txtDimensiones.getText().trim());
        e.setEspecificacionesPrincipales(txtSpecs.getText().trim());

        service.save(e);
        actualizarFiltrosDespuesDeGuardar();
        recargarDesdeInicio();
        limpiarFormulario();
        mostrarEstado(editando == null ? "Producto guardado correctamente." : "Producto actualizado.", false);
    }

    @FXML
    private void onEliminar() {
        List<Electrodomestico> seleccionados = getSeleccionados();
        if (seleccionados.isEmpty()) {
            Electrodomestico fila = tablaElectrodomesticos.getSelectionModel().getSelectedItem();
            if (fila == null) { AlertHelper.warn("Selección vacía", "Marca al menos un producto para eliminar."); return; }
            seleccionados = List.of(fila);
        }

        int n = seleccionados.size();
        if (!AlertHelper.confirm("Eliminar", "¿Eliminar " + n + " producto(s)? Esta acción no se puede deshacer.")) return;

        service.deleteAll(seleccionados);
        selectionMap.clear();
        actualizarFiltrosDespuesDeGuardar();
        recargarDesdeInicio();
        limpiarFormulario();
        mostrarEstado(n + " producto(s) eliminado(s).", false);
    }

    @FXML
    private void onLimpiarFiltros() {
        cbTipo.setValue("Todos");
        cbMarca.setValue("Todas");
    }

    @FXML
    private void onImprimirSeleccionados() {
        List<Electrodomestico> seleccionados = getSeleccionados();
        if (seleccionados.isEmpty()) {
            AlertHelper.warn("Sin selección", "Marca al menos un producto con el checkbox para imprimir.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Guardar etiquetas PDF");
        chooser.setInitialFileName("etiquetas_electrodomesticos.pdf");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File file = chooser.showSaveDialog(tablaElectrodomesticos.getScene().getWindow());
        if (file == null) return;

        try {
            pdfService.generarEtiquetasA6enA4(seleccionados, file.getAbsolutePath());
            mostrarEstado("PDF generado: " + file.getName(), false);
            AlertHelper.info("PDF Generado", "Etiquetas guardadas en:\n" + file.getAbsolutePath());
        } catch (IOException ex) {
            AlertHelper.error("Error al generar PDF", ex.getMessage());
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Helpers
    // ═════════════════════════════════════════════════════════════════════════
    private List<Electrodomestico> getSeleccionados() {
        List<Electrodomestico> result = new ArrayList<>();
        for (Electrodomestico e : items) {
            SimpleBooleanProperty prop = selectionMap.get(e.getId());
            if (prop != null && prop.get()) result.add(e);
        }
        return result;
    }

    private boolean validarFormulario() {
        if (txtTipo.getText().isBlank() || txtMarca.getText().isBlank() || txtModelo.getText().isBlank()) {
            AlertHelper.warn("Campos requeridos", "Tipo, Marca y Modelo son obligatorios.");
            return false;
        }
        try {
            new BigDecimal(txtPrecio.getText().trim().replace(",", "."));
        } catch (NumberFormatException e) {
            AlertHelper.warn("Precio inválido", "Introduce un número válido para el precio (ej: 299.99).");
            return false;
        }
        return true;
    }

    private void actualizarFiltrosDespuesDeGuardar() {
        String tipoActual  = cbTipo.getValue();
        String marcaActual = cbMarca.getValue();
        cbTipo.getItems().setAll("Todos");
        cbTipo.getItems().addAll(service.findTipos());
        cbMarca.getItems().setAll("Todas");
        cbMarca.getItems().addAll(service.findMarcas());
        cbTipo.setValue(tipoActual);
        cbMarca.setValue(marcaActual);
    }

    private void mostrarEstado(String msg, boolean error) {
        lblEstado.setText(msg);
        lblEstado.setStyle(error ? "-fx-text-fill: #e74c3c;" : "-fx-text-fill: #27ae60;");
    }
}
