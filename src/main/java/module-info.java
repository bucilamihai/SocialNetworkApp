module ir.map {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires java.sql;

    opens ir.map to javafx.fxml;
    opens ir.map.GUI to javafx.fxml;
    exports ir.map;

    opens ir.map.domain to javafx.base;
    exports ir.map.GUI;
}