package com.zestedesavoir.zestwriter.view.task;

import com.zestedesavoir.zestwriter.MainApp;
import com.zestedesavoir.zestwriter.model.MetadataContent;
import com.zestedesavoir.zestwriter.utils.Configuration;
import com.zestedesavoir.zestwriter.utils.ZdsHttp;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class DownloadContentService extends Service<Void>{

	@Override
    protected Task<Void> createTask() {
        return new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                int max = MainApp.getZdsutils().getContentListOnline().size();
                int iterations = 0;
                if (MainApp.getZdsutils().isAuthenticated()) {
                    for (MetadataContent meta : MainApp.getZdsutils().getContentListOnline()) {
                        updateMessage(Configuration.bundle.getString("ui.task.download.label")+" : " + meta.getSlug());
                        updateProgress(iterations, max);
                        MainApp.getZdsutils().downloaDraft(meta.getId(), meta.getType());
                        iterations++;
                    }

                    iterations = 0;
                    for (MetadataContent meta : MainApp.getZdsutils().getContentListOnline()) {
                        updateMessage(Configuration.bundle.getString("ui.task.unzip.label")+" : " + meta.getSlug());
                        updateProgress(iterations, max);
                        MainApp.getZdsutils().unzipOnlineContent(MainApp.getZdsutils().getOnlineContentPathDir() + File.separator + meta.getSlug() + ".zip");
                        iterations++;
                    }
                    updateMessage(Configuration.bundle.getString("ui.task.end.label"));
                    updateProgress(iterations, max);
                }
                return null;
            }
        };
    }
}
