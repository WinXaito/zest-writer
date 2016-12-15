package com.zestedesavoir.zestwriter.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zestedesavoir.zestwriter.MainApp;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.http.client.fluent.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Configuration {
    private final Logger logger;
    private Properties conf;
    private Properties actions;
    private File confFile;
    private File actionFile;
    private StorageSaver offlineSaver;
    private StorageSaver onlineSaver;
    private LocalDirectoryFactory workspaceFactory;
    private Properties props;
    public static ResourceBundle bundle;

    public Configuration(String homeDir) {
        logger = LoggerFactory.getLogger(Configuration.class);
        String appName = "zestwriter";
        String confDirPath = homeDir + File.separator + "." + appName;
        File confDir = new File(confDirPath);
        if(!confDir.exists()){
            if(!confDir.mkdir())
                logger.error("Le fichier de configuration n'a pas pu être créé");
        }

        initConf(confDirPath);
        initActions(confDirPath);
        try {
            bundle = ResourceBundle.getBundle("locales/ui", Lang.getLangFromCode(getDisplayLang()).getLocale());
        }catch(Exception e) {
            bundle = ResourceBundle.getBundle("locales/ui", Locale.FRANCE);
            logger.error("Impossible de charger la langue "+getDisplayLang());
        }
    }

    public static String getDefaultWorkspace() {
        JFileChooser fr = new JFileChooser();
        FileSystemView fw = fr.getFileSystemView();
        return fw.getDefaultDirectory().getAbsolutePath() + File.separator + "zwriter-workspace";
    }

    public static String getLastRelease() throws IOException {
        String projecUrlRelease = "https://api.github.com/repos/firm1/zest-writer/releases/latest";

        String json = Request.Get(projecUrlRelease).execute().returnContent().asString();
        ObjectMapper mapper = new ObjectMapper(); // can reuse, share globally
        Map map = mapper.readValue(json, Map.class);
        if(map.containsKey("tag_name")) {
            return (String) map.get("tag_name");
        }
        return null;
    }

    private void initActions(String confDirPath) {
        actions = new Properties();

        String actionFileName = "action.properties";
        String actionFilePath = confDirPath+File.separator+ actionFileName;
        actionFile = new File(actionFilePath);

        if(!actionFile.exists()) {
            logger.debug("le fichier des actions "+actionFile.getAbsolutePath()+" n'existe pas");
            saveActionFile();
        }
        else {
            try {
                actions.load(new FileInputStream(actionFile));
            } catch (IOException e) {
                logger.error("Impossible de charger le fichier d'actions", e);
            }
        }
    }

    private void initConf(String confDirPath) {
        String confFileName = "conf.properties";
        String confFilePath = confDirPath+File.separator+ confFileName;
        confFile = new File(confFilePath);
        // defaults config
        props = new Properties();
        try {
            props.load(MainApp.class.getClassLoader().getResourceAsStream("config.properties"));
        } catch (IOException e) {
            logger.error("", e);
        }

        conf = new Properties(props);

        if(!confFile.exists()) {
            logger.debug("le fichier de configuration "+confFile.getAbsolutePath()+" n'existe pas");
            setWorkspacePath(Configuration.getDefaultWorkspace());
            saveConfFile();
        }
        else {
            try {
                conf.load(new FileInputStream(confFile));
                loadWorkspace();
            } catch (IOException e) {
                logger.error("", e);
            }
        }

        props.entrySet().stream()
                .filter(entry -> !conf.containsKey(entry.getKey()))
                .forEach(entry -> {
                        conf.putIfAbsent(entry.getKey(), entry.getValue());
                        saveConfFile();
                    }
                );
    }

    public void saveConfFile() {
        try {
            conf.store(new FileOutputStream(confFile), "");
            logger.info("Fichier de configuration enregistré dans "+confFile.getAbsolutePath());
        } catch (IOException e) {
            logger.error("Impossible de sauvegarder le fichier de configuration", e);
        }
    }

    public void saveActionFile() {
        try {
            actions.store(new FileOutputStream(actionFile), "");
            logger.info("Fichier d'actions enregistré");
        } catch (IOException e) {
            logger.error("Impossible de sauvegarder le fichier d'actions", e);
        }
    }

    public String getPandocProvider() {
        return "http://firm1.eu/2pdf/";
    }

    public StorageSaver getOfflineSaver() {
        return offlineSaver;
    }

    public StorageSaver getOnlineSaver() {
        return onlineSaver;
    }

    public LocalDirectoryFactory getWorkspaceFactory() {
        return workspaceFactory;
    }

    public void loadWorkspace() {

        this.workspaceFactory = new LocalDirectoryFactory(getWorkspacePath());

        offlineSaver = workspaceFactory.getOfflineSaver();
        onlineSaver = workspaceFactory.getOnlineSaver();
        logger.info("Espace de travail chargé en mémoire");
    }

    /*
     * Zest-Writer data
     */
    private double getGenericDoubleDisplay(ConfigData configData) {
        if(conf.containsKey(configData.getKey())){
            if(NumberUtils.isNumber(conf.getProperty(configData.getKey())))
                return Double.parseDouble(conf.getProperty(configData.getKey()));
            else
                return Double.parseDouble(configData.getDefaultValue());
        }else{
            return Double.parseDouble(configData.getDefaultValue());
        }
    }

    private boolean getGenericBooleanDisplay(ConfigData configData) {
        if(conf.containsKey(configData.getKey())){
            return Boolean.parseBoolean(conf.getProperty(configData.getKey()));
        }else{
            return Boolean.parseBoolean(configData.getDefaultValue());
        }
    }

    public double getDisplayWindowWidth(){
        return getGenericDoubleDisplay(ConfigData.DisplayWindowWidth);
    }

    public void setDisplayWindowWidth(String windowWidth){
        conf.setProperty(ConfigData.DisplayWindowWidth.getKey(), windowWidth);
    }

    public double getDisplayWindowHeight(){
        return getGenericDoubleDisplay(ConfigData.DisplayWindowHeight);
    }

    public void setDisplayWindowHeight(String windowWidth){
        conf.setProperty(ConfigData.DisplayWindowHeight.getKey(), windowWidth);
    }

    public double getDisplayWindowPositionX(){
        return getGenericDoubleDisplay(ConfigData.DisplayWindowPositionX);
    }

    public void setDisplayWindowPositionX(String windowWidth){
        conf.setProperty(ConfigData.DisplayWindowPositionX.getKey(), windowWidth);
    }

    public double getDisplayWindowPositionY(){
        return getGenericDoubleDisplay(ConfigData.DisplayWindowPositionY);
    }

    public void setDisplayWindowPositionY(String windowWidth){
        conf.setProperty(ConfigData.DisplayWindowPositionY.getKey(), windowWidth);
    }

    /*
     * Zest-Writer options
     */
    public String getWorkspacePath(){
        String workspacePath = conf.getProperty(ConfigData.WorkspacePath.getKey());

        if(workspacePath != null && !workspacePath.isEmpty())
            return workspacePath;
        else
            return Configuration.getDefaultWorkspace();
    }

    public void setWorkspacePath(String path){
        conf.setProperty(ConfigData.WorkspacePath.getKey(), path);
    }

    public String getContentsPath(){
        if(conf.containsKey(ConfigData.ContentsPath.getKey()))
            return conf.getProperty(ConfigData.ContentsPath.getKey());
        else
            return getWorkspacePath() + "/contents";
    }

    public void setContentsPath(String path){
        conf.setProperty(ConfigData.ContentsPath.getKey(), path);
    }

    public boolean isEditorSmart(){
        return getGenericBooleanDisplay(ConfigData.EditorSmart);
    }

    public void setEditorSmart(String smart){
        conf.setProperty(ConfigData.EditorSmart.getKey(), smart);
    }

    public String getEditorFont(){
        if(conf.containsKey(ConfigData.EditorFont.getKey()))
            return conf.getProperty(ConfigData.EditorFont.getKey());
        else
            return ConfigData.EditorFont.getDefaultValue();
    }

    public void setEditorFont(String font){
        conf.setProperty(ConfigData.EditorFont.getKey(), font);
    }

    public int getEditorFontsize(){
        return (int) Math.round(getGenericDoubleDisplay(ConfigData.EditorFontSize));
    }

    public void setEditorFontSize(String fontSize){
        conf.setProperty(ConfigData.EditorFontSize.getKey(), fontSize);
    }

    public boolean isEditorToolbarView(){
        return getGenericBooleanDisplay(ConfigData.EditorToolbarView);
    }

    public void setEditorToolbarView(boolean view){
        conf.setProperty(ConfigData.EditorToolbarView.getKey(), String.valueOf(view));
    }

    public boolean isEditorLinenoView(){
        return getGenericBooleanDisplay(ConfigData.EditorLinenoView);
    }

    public void setEditorLinenoView(boolean view){
        conf.setProperty(ConfigData.EditorLinenoView.getKey(), String.valueOf(view));
    }

    public boolean isEditorRenderView(){
        return getGenericBooleanDisplay(ConfigData.EditorRenderView);
    }

    public void setEditorRenderView(boolean view){
        conf.setProperty(ConfigData.EditorRenderView.getKey(), String.valueOf(view));
    }

    public String getDisplayTheme(){
        if(conf.containsKey(ConfigData.DisplayTheme.getKey())) {
            if (Theme.getThemeFromFileName(conf.getProperty(ConfigData.DisplayTheme.getKey())) != null) {
                return conf.getProperty(ConfigData.DisplayTheme.getKey());
            }
        }
        return ConfigData.DisplayTheme.getDefaultValue();
    }

    public String getDisplayLang(){
        if(conf.containsKey(ConfigData.DisplayLang.getKey()))
            return conf.getProperty(ConfigData.DisplayLang.getKey());
        else
            return ConfigData.DisplayLang.getDefaultValue();
    }

    public void setDisplayTheme(String displayTheme){
        conf.setProperty(ConfigData.DisplayTheme.getKey(), displayTheme);
    }

    public void setDisplayLang(String displayLang){
        conf.setProperty(ConfigData.DisplayLang.getKey(), displayLang);
    }

    public boolean isDisplayWindowPersonnalDimension(){
        return getGenericBooleanDisplay(ConfigData.DisplayWindowPersonnalDimension);
    }

    public void setDisplayWindowStandardDimension(String standardDimension){
        conf.setProperty(ConfigData.DisplayWindowPersonnalDimension.getKey(), standardDimension);
    }

    public boolean isDisplayWindowPersonnalPosition(){
        return getGenericBooleanDisplay(ConfigData.DisplayWindowPersonnalPosition);
    }

    public void setDisplayWindowPersonnalPosition(String standardPosition){
        conf.setProperty(ConfigData.DisplayWindowPersonnalPosition.getKey(), standardPosition);
    }

    public boolean isDisplayWindowMaximize(){
        return getGenericBooleanDisplay(ConfigData.DisplayWindowMaximize);
    }

    public void setDisplayWindowMaximize(String maximize){
        conf.setProperty(ConfigData.DisplayWindowMaximize.getKey(), maximize);
    }

    public String getAuthentificationUsername(){
        if(conf.containsKey(ConfigData.AuthentificationUsername.getKey()))
            return conf.getProperty(ConfigData.AuthentificationUsername.getKey());
        else
            return ConfigData.AuthentificationUsername.getDefaultValue();
    }

    public void setAuthentificationUsername(String username){
        conf.setProperty(ConfigData.AuthentificationUsername.getKey(), username);
    }

    public String getAuthentificationPassword(){
        if(conf.containsKey(ConfigData.AuthentificationPassword.getKey()))
            return conf.getProperty(ConfigData.AuthentificationPassword.getKey());
        else
            return ConfigData.AuthentificationPassword.getDefaultValue();
    }

    public void setAuthentificationPassword(String password){
        conf.setProperty(ConfigData.AuthentificationPassword.getKey(), password);
    }

    public String getAdvancedApiServerProtocol(){
        if(conf.containsKey(ConfigData.AdvancedApiServerProtocol.getKey()))
            return conf.getProperty(ConfigData.AdvancedApiServerProtocol.getKey());
        else
            return ConfigData.AdvancedApiServerProtocol.getDefaultValue();
    }

    public void setAdvancedApiServerProtocol(String protocol){
        conf.setProperty(ConfigData.AdvancedApiServerProtocol.getKey(), protocol);
    }

    public String getAdvancedApiServerHost(){
        if(conf.containsKey(ConfigData.AdvancedApiServerHost.getKey()))
            return conf.getProperty(ConfigData.AdvancedApiServerHost.getKey());
        else
            return ConfigData.AdvancedApiServerHost.getDefaultValue();
    }

    public void setAdvancedApiServerHost(String host){
        conf.setProperty(ConfigData.AdvancedApiServerHost.getKey(), host);
    }

    public String getAdvancedApiServerPort(){
        if(conf.containsKey(ConfigData.AdvancedApiServerPort.getKey()))
            return conf.getProperty(ConfigData.AdvancedApiServerPort.getKey());
        else
            return ConfigData.AdvancedApiServerPort.getDefaultValue();
    }

    public void setAdvancedApiServerPort(String port){
        conf.setProperty(ConfigData.AdvancedApiServerPort.getKey(), port);
    }

    public String getAdvancedServerProtocol(){
        if(conf.containsKey(ConfigData.AdvancedServerProtocol.getKey()))
            return conf.getProperty(ConfigData.AdvancedServerProtocol.getKey());
        else
            return ConfigData.AdvancedServerProtocol.getDefaultValue();
    }

    public void setAdvancedServerProtocol(String protocol){
        conf.setProperty(ConfigData.AdvancedServerProtocol.getKey(), protocol);
    }

    public String getAdvancedServerHost(){
        if(conf.containsKey(ConfigData.AdvancedServerHost.getKey()))
            return conf.getProperty(ConfigData.AdvancedServerHost.getKey());
        else
            return ConfigData.AdvancedServerHost.getDefaultValue();
    }

    public void setAdvancedServerHost(String host){
        conf.setProperty(ConfigData.AdvancedServerHost.getKey(), host);
    }

    public String getAdvancedServerPort(){
        if(conf.containsKey(ConfigData.AdvancedServerPort.getKey())) {
            return conf.getProperty(ConfigData.AdvancedServerPort.getKey());
        }
        else {
            return ConfigData.AdvancedServerPort.getDefaultValue();
        }
    }

    public void setAdvancedServerPort(String port){
        conf.setProperty(ConfigData.AdvancedServerPort.getKey(), port);
    }

    public void resetAuthentification(){
        setAuthentificationUsername("");
        setAuthentificationPassword("");
        saveConfFile();
    }

    public void resetAllOptions(){
        for(ConfigData opt : ConfigData.values()){
            conf.setProperty(opt.getKey(), opt.getDefaultValue());
        }

        saveConfFile();
    }

    public Properties getProps() {
        return props;
    }

    public List<String> getActions() {
        String value = actions.getProperty(ActionData.LastProjects.getKey());
        if(value != null && ! value.trim().equals("")) {
            return Arrays.asList(value.split(","));
        } else {
            return new ArrayList<>();
        }
    }

    public void addActionProject(String projectFileName) {
        List<String> existant = getActions();
        List<String> recents = new ArrayList<>(existant);
        if(recents.contains(projectFileName)) {
            recents.remove(projectFileName);
        }
        recents.add(0, projectFileName);

        actions.put(ActionData.LastProjects.getKey(), recents.stream().limit(5).map(Object::toString).collect(Collectors.joining(",")));
        saveActionFile();
    }

    public void delActionProject(String projectFileName) {
        List<String> existant = getActions();
        List<String> recents = new ArrayList<>(existant);
        if(recents.contains(projectFileName)) {
            recents.remove(projectFileName);
        }
        actions.put(ActionData.LastProjects.getKey(), recents.stream().limit(5).map(Object::toString).collect(Collectors.joining(",")));
        saveActionFile();
    }

    public enum ActionData{
        LastProjects("content.open", "");
        private String key;
        private String defaultValue;

        ActionData(String key, String defaultValue){
            this.key = key;
            this.defaultValue = defaultValue;
        }

        public String getKey(){
            return key;
        }

        public String getDefaultValue(){
            return defaultValue;
        }
    }

    public enum ConfigData{
        DisplayWindowWidth("data.display.window.width", "1000"),
        DisplayWindowHeight("data.display.window.height", "600"),
        DisplayWindowPositionX("data.display.window.position.x", "0"),
        DisplayWindowPositionY("data.display.window.position.y", "0"),

        WorkspacePath("options.workspace.path", ""),
        ContentsPath("options.workspace.contents.path", ""),
        EditorSmart("options.editor.smart", "true"),
        EditorFont("options.editor.font", "Fira Mono"),
        EditorFontSize("options.editor.fontSize", "14"),
        EditorToolbarView("options.editor.toolbar.view", "true"),
        EditorLinenoView("options.editor.lineno.view", "true"),
        EditorRenderView("options.editor.render.view", "true"),
        DisplayTheme("options.display.theme", "light.css"),
        DisplayLang("options.display.lang", Locale.FRANCE.toString()),
        DisplayWindowPersonnalDimension("options.display.window.standardDimension", "true"),
        DisplayWindowPersonnalPosition("options.display.window.standardPosition", "true"),
        DisplayWindowMaximize("options.display.window.maximize", "false"),
        AuthentificationUsername("options.authentification.username", ""),
        AuthentificationPassword("options.authentification.password", ""),
        AdvancedApiServerProtocol("options.advanced.protocol", "http"),
        AdvancedApiServerHost("options.advanced.host", "winxaito.com"),
        AdvancedApiServerUri("options.advanced.host", "api"),
        AdvancedApiServerPort("options.advanced.port", "80"),
        AdvancedServerProtocol("server.protocol", "https"),
        AdvancedServerHost("server.host", "zestedesavoir.com"),
        AdvancedServerPort("server.port", "443");

        private String key;
        private String defaultValue;

        ConfigData(String key, String defaultValue){
            this.key = key;
            this.defaultValue = defaultValue;
        }

        public String getKey(){
            return key;
        }

        public String getDefaultValue(){
            return defaultValue;
        }
    }
}
