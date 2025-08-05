# jbudget

## How to launch

Create a runtime with javafx:
```shell
jlink.exe --module-path "C:\YIE\tools\javafx\openjfx-24.0.2_windows-x64_bin-jmods\javafx-jmods-24.0.2" --add-modules javafx.controls,javafx.fxml --output jbudget-runtime
```

Then run the application:
```shell
.\jbudget-runtime\bin\java.exe -jar .\target\jbudget-1.0-SNAPSHOT.jar
```