package com.zestedesavoir.zestwriter.view.com;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zestedesavoir.zestwriter.MainApp;
import com.zestedesavoir.zestwriter.model.*;
import com.zestedesavoir.zestwriter.utils.Configuration;
import com.zestedesavoir.zestwriter.utils.Corrector;
import com.zestedesavoir.zestwriter.utils.ZdsHttp;
import com.zestedesavoir.zestwriter.utils.readability.Readability;
import com.zestedesavoir.zestwriter.view.MdTextController;
import com.zestedesavoir.zestwriter.view.dialogs.BaseDialog;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Side;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MdTreeCell extends TreeCell<ContentNode>{
	private MdTextController index;
	private String baseFilePath;
	private final Logger logger;
    private ContextMenu addMenu = new ContextMenu();
    private Content content;


    public MdTreeCell(MdTextController index) {
		this.index = index;
        if(index.getMainApp().getContents().size() > 0) {
            this.content = index.getMainApp().getContents().stream().findFirst().get();
            this.baseFilePath = this.content.getBasePath();
        }
        else {
            this.content = null;
        }
		this.logger = LoggerFactory.getLogger(getClass());
	}

    private void closeTab(Textual closedTextual) {
        for (Map.Entry<Textual, Tab> entry : index.getMainApp().getExtracts().entrySet()) {
            if (entry.getKey().equals(closedTextual)) {
                index.getEditorList().getTabs().remove(entry.getValue());
                index.getMainApp().getExtracts().remove(entry);
                break;
            }
        }
    }

	public void initContextMenu(ContentNode item) {
        MenuItem addMenuItem1 = new MenuItem(Configuration.bundle.getString("ui.actions.add_extract.label"));
        MenuItem addMenuItem2 = new MenuItem(Configuration.bundle.getString("ui.actions.add_container.label"));
        MenuItem addMenuItem3 = new MenuItem(Configuration.bundle.getString("ui.actions.rename.label"));
        MenuItem addMenuItem4 = new MenuItem(Configuration.bundle.getString("ui.actions.delete.label"));
        MenuItem addMenuItem5 = new MenuItem(Configuration.bundle.getString("ui.actions.edit.label"));
        MenuItem addMenuItem6 = new MenuItem(Configuration.bundle.getString("ui.actions.merge_extracts.label"));
        Menu menuStats = new Menu(Configuration.bundle.getString("ui.actions.stats.label"));
        MenuItem menuStatCountHisto = new MenuItem(Configuration.bundle.getString("ui.actions.stats.count.histo"));
        MenuItem menuStatReadability = new MenuItem(Configuration.bundle.getString("ui.actions.stats.readability"));
        MenuItem menuStatMistakes = new MenuItem(Configuration.bundle.getString("ui.actions.stats.mistake"));
        menuStats.getItems().add(menuStatCountHisto);
        menuStats.getItems().add(menuStatReadability);
        menuStats.getItems().add(menuStatMistakes);
        addMenuItem1.setGraphic(IconFactory.createFileIcon());
        addMenuItem2.setGraphic(IconFactory.createAddFolderIcon());
        addMenuItem3.setGraphic(IconFactory.createEditIcon());
        addMenuItem4.setGraphic(IconFactory.createRemoveIcon());
        addMenuItem5.setGraphic(IconFactory.createEditIcon());
        addMenuItem6.setGraphic(IconFactory.createMoveIcon());
        menuStats.setGraphic(IconFactory.createStatsIcon());
        menuStatCountHisto.setGraphic(IconFactory.createStatsHistoIcon());
        menuStatReadability.setGraphic(IconFactory.createStatsHistoIcon());
        menuStatMistakes.setGraphic(IconFactory.createAbcIcon());
        addMenu.getItems().clear();

        if(item instanceof Container) {
            addMenu.getItems().add(addMenuItem1);
            addMenu.getItems().add(addMenuItem2);
            if (!item.canTakeContainer(((Content) index.getSummary().getRoot().getValue()))) {
                addMenuItem2.setDisable(true);
            }
            if (!item.canTakeExtract()) {
                addMenuItem1.setDisable(true);
            }
        }
        if (item.isEditable()) {
            addMenu.getItems().add(addMenuItem3);
        }
        if (item instanceof Content) {
            addMenu.getItems().add(addMenuItem5);
            if(((Content) item).isArticle()) {
                addMenuItem2.setDisable(true);
                addMenuItem6.setDisable(true);
            }
        }
        if(item instanceof Container) {
            addMenu.getItems().add(new SeparatorMenuItem());
            addMenu.getItems().add(menuStats);
        }
        if (item.canMergeExtracts((Content) index.getSummary().getRoot().getValue())) {
            addMenu.getItems().add(new SeparatorMenuItem());
            addMenu.getItems().add(addMenuItem6);
        }
        if (item.canDelete()) {
            addMenu.getItems().add(new SeparatorMenuItem());
            addMenu.getItems().add(addMenuItem4);
        }

        addMenuItem4.setOnAction(t -> {
            Alert alert = new CustomAlert(AlertType.CONFIRMATION);
            alert.setTitle(Configuration.bundle.getString("ui.dialog.delete.title"));
            alert.setHeaderText(Configuration.bundle.getString("ui.dialog.delete.header"));
            alert.setContentText(Configuration.bundle.getString("ui.dialog.delete.text"));

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                logger.debug("Tentative de suppression");
                if(getTreeItem().getValue() instanceof Content) {
                    FunctionTreeFactory.clearContent(index.getMainApp().getExtracts(), index.getMainApp().getIndex().getEditorList(), () -> {
                        Content deleteContent = (Content) getTreeItem().getValue();
                        // delete last projects
                        MainApp.config.delActionProject(deleteContent.getFilePath());
                        // delete physically file
                        deleteContent.delete();
                        // delete in logical tree
                        index.getMainApp().getContents().clear();
                        index.refreshRecentProject();
                        return null;
                    });
                } else {
                    // close tabs
                    if(getTreeItem().getValue() instanceof Textual) {
                        closeTab((Textual) getTreeItem().getValue());
                    } else if (getTreeItem().getValue() instanceof Container) {
                        Container deleteContainer = (Container) getTreeItem().getValue();
                        deleteContainer.doOnTextual(textual -> {
                            closeTab(textual);
                            return null;
                        });
                    }
                    // delete in logical tree
                    Container parentContainer = (Container) getTreeItem().getParent().getValue();
                    parentContainer.getChildren().remove(getItem());
                    // delete in gui tree
                    getTreeItem().getParent().getChildren().remove(getTreeItem());
                    // delete physically file
                    getItem().delete();
                    saveManifestJson();
                }
                index.refreshRecentProject();
            }
        });

        addMenuItem1.setOnAction(t -> {
            logger.debug("Tentative d'ajout d'un nouvel extrait");
            TextInputDialog dialog = new TextInputDialog(Configuration.bundle.getString("ui.dialog.add_extract"));
            Extract extract;
            dialog.setTitle(Configuration.bundle.getString("ui.dialog.add_extract.title"));
            dialog.setHeaderText(Configuration.bundle.getString("ui.dialog.add_extract.header"));
            dialog.setContentText(Configuration.bundle.getString("ui.dialog.add_extract.text")+" :");

            Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                extract = new Extract("extract",
                        ZdsHttp.toSlug(result.get()),
                        result.get(),
                        FunctionTreeFactory.getUniqueFilePath(getItem().getFilePath() + "/" + ZdsHttp.toSlug(result.get()), "md").substring(baseFilePath.length()+1));
                extract.setRootContent(content, baseFilePath);
                ((Container)getItem()).getChildren().add(extract);
                // create file
                File extFile = new File(extract.getFilePath());
                if (!extFile.exists()) {
                    try {
                        extFile.createNewFile();
                    } catch (IOException e) {
                        logger.error(e.getMessage(), e);
                    }
                }
                saveManifestJson();
                index.openContent(content);
            }
        });

        addMenuItem2.setOnAction(t -> {
            logger.debug("Tentative d'ajout d'un nouveau conteneur");
            TextInputDialog dialog = new TextInputDialog(Configuration.bundle.getString("ui.dialog.add_container"));

            dialog.setTitle(Configuration.bundle.getString("ui.dialog.add_container.title"));
            dialog.setHeaderText(Configuration.bundle.getString("ui.dialog.add_container.header"));
            dialog.setContentText(Configuration.bundle.getString("ui.dialog.add_container.text")+" :");

            Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                String slug = ZdsHttp.toSlug(result.get());
                String baseSlug = FunctionTreeFactory.getUniqueDirPath(getItem().getFilePath() + "/" + slug);
                Container container = new Container("container",
                        slug,
                        result.get(),
                        (baseSlug + "/" + "introduction.md").substring(baseFilePath.length()+1),
                        (baseSlug + "/" + "conclusion.md").substring(baseFilePath.length()+1),
                        new ArrayList<>());
                container.setBasePath(baseFilePath);
                ((Container)getItem()).getChildren().add(container);

                // create files
                File dirFile = new File(container.getFilePath());
                File introFile = new File(container.getIntroduction().getFilePath());
                File concluFile = new File(container.getConclusion().getFilePath());

                if (!dirFile.exists() && !dirFile.isDirectory()) {
                    dirFile.mkdir();
                }
                try {
                    if (!introFile.exists()) {
                        introFile.createNewFile();
                    }
                } catch (IOException e) {
                    logger.error("Erreur lors de la créeation de "+introFile.getAbsolutePath(), e);
                }
                try {
                    if (!concluFile.exists()) {
                        concluFile.createNewFile();
                    }
                } catch (IOException e) {
                    logger.error("Erreur lors de la créeation de "+concluFile.getAbsolutePath(), e);
                }
                saveManifestJson();
                index.openContent(content);
            }
        });

        addMenuItem3.setOnAction(t -> {
            logger.debug("Tentative de renommage d'un conteneur ou extrait");
            TreeItem<ContentNode> item1 = index.getSummary().getSelectionModel().getSelectedItem();
            TextInputDialog dialog = new TextInputDialog(item1.getValue().getTitle());
            dialog.setTitle(Configuration.bundle.getString("ui.dialog.rename.title")+"  " + item1.getValue().getTitle());
            dialog.setHeaderText(Configuration.bundle.getString("ui.dialog.rename.header"));
            dialog.setContentText(Configuration.bundle.getString("ui.dialog.rename.text")+" : ");

            Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                if (!result.get().trim().equals("")) {
                    for(Map.Entry<Textual, Tab> entry:index.getMainApp().getExtracts().entrySet()) {
                        if(entry.getKey().equals((Textual) item1.getValue())) {
                            entry.getValue().setText(result.get());
                            break;
                        }
                    }
                    item1.getValue().renameTitle(result.get());
                    saveManifestJson();
                    index.openContent(content);
                }
            }

        });

        addMenuItem5.setOnAction(t -> {
            logger.debug("Tentative d'édition d'un contenu");
            try {
                Map<String,Object> paramContent= FunctionTreeFactory.initContentDialog(content);
                if(paramContent != null) {
                    index.getSummary().getRoot().getValue().renameTitle(paramContent.get("title").toString());
                    ((Content) index.getSummary().getRoot().getValue()).setDescription(paramContent.get("description").toString());
                    ((Content) index.getSummary().getRoot().getValue()).setType(paramContent.get("type").toString());
                    ((Content) index.getSummary().getRoot().getValue()).setLicence(paramContent.get("licence").toString());
                    baseFilePath = ((Content) index.getSummary().getRoot().getValue()).getBasePath();
                    saveManifestJson();
                    index.openContent(content);
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });

        addMenuItem6.setOnAction(t -> {
            logger.debug("Tentative de déplacement des extraits");
            TextInputDialog dialog = new TextInputDialog(Configuration.bundle.getString("ui.dialog.add_container"));

            dialog.setTitle(Configuration.bundle.getString("ui.dialog.add_container.title"));
            dialog.setHeaderText(Configuration.bundle.getString("ui.dialog.add_container.header"));
            dialog.setContentText(Configuration.bundle.getString("ui.dialog.add_container.text")+" :");

            Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                // lists extracts
                List<Extract> extracts = ((Container)getItem()).getChildren().stream()
                        .map(ext -> (Extract) ext)
                        .collect(Collectors.toList());

                String slug = ZdsHttp.toSlug(result.get());
                String baseSlug = FunctionTreeFactory.getUniqueDirPath(getItem().getFilePath() + "/" + slug);
                Container container = new Container("container",
                        slug,
                        result.get(),
                        (baseSlug + "/" + "introduction.md").substring(baseFilePath.length()+1),
                        (baseSlug + "/" + "conclusion.md").substring(baseFilePath.length()+1),
                        new ArrayList<>());
                container.setBasePath(baseFilePath);
                ((Container)getItem()).getChildren().add(container);

                // create files
                File dirFile = new File(container.getFilePath());
                File introFile = new File(container.getIntroduction().getFilePath());
                File concluFile = new File(container.getConclusion().getFilePath());

                if (!dirFile.exists() && !dirFile.isDirectory()) {
                    dirFile.mkdir();
                }
                try {
                    if (!introFile.exists()) {
                        introFile.createNewFile();
                    }
                } catch (IOException e) {
                    logger.error("Erreur lors de la création de "+introFile.getAbsolutePath(), e);
                }
                try {
                    if (!concluFile.exists()) {
                        concluFile.createNewFile();
                    }
                } catch (IOException e) {
                    logger.error("Erreur lors de la créeation de "+concluFile.getAbsolutePath(), e);
                }

                // move physical file to new directory
                extracts.stream()
                        .forEach(extract -> {
                            try {
                                FileUtils.moveFileToDirectory(new File(extract.getFilePath()), new File(container.getFilePath()), false);
                            } catch (IOException e) {
                                logger.error("Erreur lors du déplacement d'un extrait", e);
                            }
                        });
                // move logical file to new directory
                extracts.stream()
                        .forEach(extract -> {
                            ((Container)getItem()).getChildren().remove(extract);
                            String oldText = extract.getText();
                            if(oldText.indexOf("/") != -1) {
                                oldText = oldText.substring(oldText.indexOf("/")+1);
                            }
                            extract.setText((baseSlug+"/"+oldText).substring(baseFilePath.length()+1));

                        });
                container.getChildren().addAll(extracts);

                saveManifestJson();
                index.openContent(content);
            }
        });

        menuStatCountHisto.setOnAction(t -> {
            logger.debug("Tentative de calcul des statistiques de type histogramme");
            BaseDialog dialog = new BaseDialog(Configuration.bundle.getString("ui.actions.stats.label"), Configuration.bundle.getString("ui.actions.stats.header")+" "+getItem().getTitle());
            dialog.getDialogPane().setPrefSize(800, 600);
            dialog.getDialogPane().getButtonTypes().addAll(new ButtonType(Configuration.bundle.getString("ui.actions.stats.close"), ButtonBar.ButtonData.CANCEL_CLOSE));

            // draw
            final CategoryAxis xAxis = new CategoryAxis();
            final NumberAxis yAxis = new NumberAxis();
            final BarChart<String,Number> barChart = new BarChart<>(xAxis, yAxis);
            barChart.setCategoryGap(5);
            barChart.setBarGap(5);

            xAxis.setLabel(Configuration.bundle.getString("ui.actions.stats.xaxis"));
            yAxis.setLabel(Configuration.bundle.getString("ui.actions.stats.yaxis"));

            XYChart.Series series1 = new XYChart.Series();
            series1.setName(Configuration.bundle.getString("ui.actions.stats.type.histogram"));
            Container container = (Container) getItem();
            Function<Textual, Integer> performCount = (Textual ch) -> {
                String md = ch.readMarkdown();
                Readability readText = new Readability(md);
                return readText.getCharacters();
            };
            Map<Textual, Integer> stat = container.doOnTextual(performCount);
            for(Map.Entry<Textual, Integer> st:stat.entrySet()) {
                if(!(st.getKey() instanceof MetaAttribute)) {
                    series1.getData().add(new XYChart.Data(st.getKey().getTitle(), st.getValue()));
                } else {
                    MetaAttribute attribute = (MetaAttribute) st.getKey();
                    series1.getData().add(new XYChart.Data(attribute.getTitle()+ " (" + attribute.getParent().getTitle() + ")", st.getValue()));
                }
            }
            barChart.getData().addAll(series1);
            dialog.getDialogPane().setContent(barChart);
            dialog.setResizable(true);
            dialog.showAndWait();
        });

        menuStatReadability.setOnAction(t -> {
            logger.debug("Tentative de calcul des statistiques de lisiblité");
            BaseDialog dialog = new BaseDialog(Configuration.bundle.getString("ui.actions.stats.label"), Configuration.bundle.getString("ui.actions.stats.header")+" "+getItem().getTitle());
            dialog.getDialogPane().setPrefSize(800, 600);
            dialog.getDialogPane().getButtonTypes().addAll(new ButtonType(Configuration.bundle.getString("ui.actions.stats.close"), ButtonBar.ButtonData.CANCEL_CLOSE));

            // draw
            final CategoryAxis xAxis = new CategoryAxis();
            final NumberAxis yAxis = new NumberAxis();
            final LineChart<String,Number> lineChart = new LineChart<>(xAxis, yAxis);
            lineChart.setTitle(Configuration.bundle.getString("ui.actions.stats.readability"));

            xAxis.setLabel(Configuration.bundle.getString("ui.actions.stats.xaxis"));
            yAxis.setLabel(Configuration.bundle.getString("ui.actions.readable.yaxis"));

            XYChart.Series series1 = new XYChart.Series();
            XYChart.Series series2 = new XYChart.Series();
            series1.setName(Configuration.bundle.getString("ui.menu.edit.readable.gunning_index"));
            series2.setName(Configuration.bundle.getString("ui.menu.edit.readable.flesch_index"));
            Container container = (Container) getItem();
            Function<Textual, Double> performGuning = (Textual ch) -> {
                String md = ch.readMarkdown();
                try {
                    Readability readText = new Readability(md);
                    return readText.getGunningFog();
                } catch(Exception e) {
                    return 0.0;
                }
            };
            Function<Textual, Double> performFlesch = (Textual ch) -> {
                String md = ch.readMarkdown();
                try {
                    Readability readText = new Readability(md);
                    return readText.getFleschReadingEase();
                } catch(Exception e) {
                    return 0.0;
                }
            };
            Map<Textual, Double> statG = container.doOnTextual(performGuning);
            Map<Textual, Double> statF = container.doOnTextual(performFlesch);
            for(Map.Entry<Textual, Double> st:statG.entrySet()) {
                if(!(st.getKey() instanceof MetaAttribute)) {
                    series1.getData().add(new XYChart.Data(st.getKey().getTitle(), st.getValue()));
                } else {
                    MetaAttribute attribute = (MetaAttribute) st.getKey();
                    series1.getData().add(new XYChart.Data(attribute.getTitle()+ " (" + attribute.getParent().getTitle() + ")", st.getValue()));
                }
            }
            for(Map.Entry<Textual, Double> st:statF.entrySet()) {
                if(!(st.getKey() instanceof MetaAttribute)) {
                    series2.getData().add(new XYChart.Data(st.getKey().getTitle(), st.getValue()));
                } else {
                    MetaAttribute attribute = (MetaAttribute) st.getKey();
                    series2.getData().add(new XYChart.Data(attribute.getTitle()+ " (" + attribute.getParent().getTitle() + ")", st.getValue()));
                }
            }
            lineChart.getData().addAll(series1, series2);
            dialog.getDialogPane().setContent(lineChart);
            dialog.setResizable(true);
            dialog.showAndWait();
        });

        menuStatMistakes.setOnAction(t -> {
            logger.debug("Tentative de calcul du nombre de fautes");
            BaseDialog dialog = new BaseDialog(Configuration.bundle.getString("ui.actions.stats.label"), Configuration.bundle.getString("ui.actions.stats.header")+" "+getItem().getTitle());
            dialog.getDialogPane().setPrefSize(800, 600);
            dialog.getDialogPane().getButtonTypes().addAll(new ButtonType(Configuration.bundle.getString("ui.actions.stats.close"), ButtonBar.ButtonData.CANCEL_CLOSE));

            // draw
            final CategoryAxis xAxis = new CategoryAxis();
            final NumberAxis yAxis = new NumberAxis();
            final LineChart<String,Number> lineChart = new LineChart<>(xAxis, yAxis);
            lineChart.setTitle(Configuration.bundle.getString("ui.actions.stats.mistake"));

            xAxis.setLabel(Configuration.bundle.getString("ui.actions.stats.xaxis"));
            yAxis.setLabel(Configuration.bundle.getString("ui.actions.stats.mistakes.yaxis"));

            XYChart.Series series1 = new XYChart.Series();
            series1.setName(Configuration.bundle.getString("ui.actions.stats.mistakes.yaxis"));
            Container container = (Container) getItem();

            Corrector corrector = new Corrector();
            Function<Textual, Integer> performCorrection = (Textual ch) -> {
                String md = ch.readMarkdown();
                try {
                    return corrector.countMistakes(index, md);
                } catch(Exception e) {
                    return 0;
                }
            };
            Map<Textual, Integer> statMistake = container.doOnTextual(performCorrection);
            for(Map.Entry<Textual, Integer> st:statMistake.entrySet()) {
                if(!(st.getKey() instanceof MetaAttribute)) {
                    series1.getData().add(new XYChart.Data(st.getKey().getTitle(), st.getValue()));
                }
            }
            lineChart.getData().addAll(series1);
            dialog.getDialogPane().setContent(lineChart);
            dialog.setResizable(true);
            dialog.showAndWait();
        });
    }

    protected void updateItem(ContentNode item, boolean empty) {
        super.updateItem(item, empty);

        if (empty) {
            setText(null);
            setGraphic(null);
        } else {
            setText(getString());
            setGraphic(getItem().buildIcon());
            setContextMenu(addMenu);
            initContextMenu(item);
        }
    }

    private String getString() {
        return getItem() == null ? "" : getItem().getTitle();
    }

    public void saveManifestJson() {
        Content c = (Content) index.getSummary().getRoot().getValue();

        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(baseFilePath + File.separator + "manifest.json"), c);
            logger.info("Fichier manifest sauvegardé");
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }
}
