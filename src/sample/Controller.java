package sample;

import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;

public class Controller {

    Connection connection = null; // database connection
    File outputFilefile = null;

    String sepSymbol= "\\";
    Boolean isWindows = true;
    @FXML
    private Button btninputChooseDb;
    @FXML
    private Button btnStart;
    @FXML
    private Button btnOutputFile;
    @FXML
    private Label labelDBFilePath;
    @FXML
    private Label labelOutputFile;
    @FXML
    private Label labelMsg;
    @FXML
    private RadioButton radioW;
    @FXML
    private RadioButton radioL;



    @FXML
    public void initialize(){
        /* handler of load button */


        btninputChooseDb.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                FileChooser fileChooser = new FileChooser();

                //Set extension filter
                FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("sqllite3 files (*.sqlite3)", "*.sqlite3");
                fileChooser.getExtensionFilters().add(extFilter);

                //Show save file dialog
                File file = fileChooser.showOpenDialog(new Stage());

                if(file != null){

                    try{
                        connection = DriverManager.getConnection( "jdbc:sqlite:"+  file.getAbsolutePath() );
                        if ( connection != null ){
                            labelDBFilePath.setText("Connection success. DB file: " +  file.getAbsolutePath());
                        }
                    }
                    catch ( Exception ex ) {
                        labelDBFilePath.setText(ex.getClass().getName() + ": " + ex.getMessage());
                    }
                }
            }
        });

        btnOutputFile.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                FileChooser fileChooser = new FileChooser();

                //Set extension filter
                FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("JSON files (*.json)", "*.json");
                fileChooser.getExtensionFilters().add(extFilter);

                //Show save file dialog
                outputFilefile = fileChooser.showSaveDialog(new Stage());

                if(outputFilefile != null){
                    SaveFile(outputFilefile);
                }

                labelMsg.setText("Output File Created");
            }
        });

        btnStart.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                labelMsg.setText("Messages:");
                labelMsg.setTextFill(Color.BLACK);
                if(connection==null){
                    labelMsg.setText("Select Database!");
                    labelMsg.setTextFill(Color.RED);
                }
                else if(outputFilefile==null){
                    labelMsg.setText("Select Output file!");
                    labelMsg.setTextFill(Color.RED);
                }
                else {
                    try {
                        parseDb();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
        });


    }

    private void parseDb() throws SQLException {
 /*       ArrayList exportItemsNormal = new ArrayList();  // we cannot use it, because we cant do somethink with it */
        if(radioW.isSelected()){
            sepSymbol= "\\";
            isWindows=true;
        }
        else{
            sepSymbol = "/";
            isWindows = false;
        }

        JSONArray exportItemsMultiple = new JSONArray();
        JSONArray exportItemsTrashFolder = new JSONArray();

        JSONArray exportItemsNotLgOrCenter = new JSONArray();
        JSONArray exportItemsExtra = new JSONArray();
        JSONArray exportItemsTrashImages = new JSONArray();

        JSONArray exportItemsNO_THUMBNAILS = new JSONArray();
        JSONArray exportItemsINVALID_FOLDER_CHECKED = new JSONArray();

        Statement stmt = null;
        stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery( "SELECT * FROM workspace_directory WHERE classifications_amount > 0;" );

        int root_dir_id = -1 ;
        while ( rs.next() ) {

            int id = rs.getInt("id");
            int classifications_amount  = rs.getInt("classifications_amount");
            root_dir_id  = rs.getInt("root_dir_id");
            String  path = rs.getString("path");
            String directory_class = rs.getString("directory_class");

            if(directory_class.equals("Multiple")){
                exportItemsMultiple.put(path);
            }
            else if(directory_class.equals("TrashFolder")){
                exportItemsTrashFolder.put(path);
            }
            else if(directory_class.equals("NO_THUMBNAILS")){
                exportItemsNO_THUMBNAILS.put(path);
            }
            else if(directory_class.equals("INVALID_FOLDER_CHECKED")){
                exportItemsINVALID_FOLDER_CHECKED.put(path);
            }
            else if(directory_class.equals("NotLgOrCenter") || directory_class.equals("Extra") || directory_class.equals("TrashImages") ){
                Statement stmtfiles = connection.createStatement();
                ResultSet items = stmtfiles.executeQuery( "SELECT * FROM workspace_directoryitem WHERE dir_id = "+id+"  AND is_bad = 1;" );
                while (items.next()){
                    String filename = items.getString("name");
                    if(directory_class.equals("NotLgOrCenter")) exportItemsNotLgOrCenter.put(path+ sepSymbol + filename);
                    else if(directory_class.equals("Extra")) exportItemsExtra.put(path+ sepSymbol + filename);
                    else if(directory_class.equals("TrashImages")) exportItemsTrashImages.put(path+ sepSymbol + filename);
                }
                stmtfiles.close();
                items.close();
            }

        }


        stmt.close();
        rs.close();
        writeToJson(exportItemsMultiple,exportItemsTrashFolder,exportItemsNotLgOrCenter,exportItemsExtra,exportItemsTrashImages,exportItemsNO_THUMBNAILS,exportItemsINVALID_FOLDER_CHECKED);

    }

    private void writeToJson(JSONArray exportItemsMultiple,JSONArray exportItemsTrashFolder,JSONArray exportItemsNotLgOrCenter,JSONArray exportItemsExtra,JSONArray exportItemsTrashImages,JSONArray exportItemsNO_THUMBNAILS,JSONArray exportItemsINVALID_FOLDER_CHECKED) {

        if(!outputFilefile.exists()){
            SaveFile(outputFilefile);
        }
        JSONObject obj = new JSONObject();

        FileWriter file = null;
        try {
            obj.put("Multiple", exportItemsMultiple );
            obj.put("TrashFolder", exportItemsTrashFolder );
            obj.put("NotLgOrCenter", exportItemsNotLgOrCenter );
            obj.put("Extra", exportItemsExtra );
            obj.put("TrashImages", exportItemsTrashImages );

            obj.put("NO_THUMBNAILS", exportItemsNO_THUMBNAILS );
            obj.put("INVALID_FOLDER_CHECKED", exportItemsINVALID_FOLDER_CHECKED );
            file = new FileWriter(outputFilefile);
            obj.write(file);
            file.flush();
            file.close();
            labelMsg.setText("Done");
            labelMsg.setTextFill(Color.GREEN);
        } catch (IOException e) {
            labelMsg.setText(e.toString());
            labelMsg.setTextFill(Color.RED);
        } catch (JSONException e) {
            labelMsg.setText(e.toString());
            labelMsg.setTextFill(Color.RED);
        }


    }

    private void SaveFile(File file){
        try {
            FileWriter fileWriter = null;

            fileWriter = new FileWriter(file);
            // fileWriter.write(content);
            fileWriter.close();
            labelOutputFile.setText("output file: " + file.getAbsolutePath() );
        } catch (IOException ex) {

        }

    }


}