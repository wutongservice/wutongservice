package com.borqs.server.platform.web.doc.html;


import com.borqs.server.platform.io.Charsets;
import com.borqs.server.platform.util.VfsHelper;
import com.borqs.server.platform.web.doc.HttpApiDoclet;
import com.borqs.server.platform.web.template.FreeMarkerLayout;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class HtmlOutput implements HttpApiDoclet.Output {
    @Override
    public void output(HttpApiDoclet.ApiDocs docs, String outputDir, String[] args) throws Exception {
        if (new File(outputDir).exists())
            FileUtils.cleanDirectory(new File(outputDir));

        FileUtils.forceMkdir(new File(outputDir));
        FreeMarkerLayout layout = new FreeMarkerLayout(HtmlOutput.class);

        // list
        String listHtml = makeListHtml(docs, layout);
        writeText(FilenameUtils.concat(outputDir, "list.html"), listHtml);

        // about
        String aboutHtml = makeAboutHtml(docs, layout);
        writeText(FilenameUtils.concat(outputDir, "about.html"), aboutHtml);

        // top bar
        String topHtml = makeTopHtml(docs, layout);
        writeText(FilenameUtils.concat(outputDir, "top.html"), topHtml);

        // routes
        for (HttpApiDoclet.ApiDoc doc : docs.docs) {
            String routeHtml = makeRouteHtml(doc, layout);
            writeText(FilenameUtils.concat(outputDir, doc.getRouteHtmlFile()), routeHtml);
        }

        // index
        String indexHtml = makeIndexHtml(docs, layout);
        writeText(FilenameUtils.concat(outputDir, "index.html"), indexHtml);

        // css
        writeText(FilenameUtils.concat(outputDir, "doc.css"), VfsHelper.loadTextInClasspath(HtmlOutput.class, "doc.css"));
    }


    private void writeText(String file, String text) throws IOException {
        OutputStreamWriter w = null;
        try {
            w = new OutputStreamWriter(new FileOutputStream(file), Charsets.DEFAULT);
            IOUtils.write(text, w);
            System.out.println("Write: " + file);
        } finally {
            IOUtils.closeQuietly(w);
        }
    }

    private static String makeListHtml(HttpApiDoclet.ApiDocs docs, FreeMarkerLayout layout) {
        String body = layout.merge("list.ftl", new Object[][] {
                {"docs", docs},
        });
        return layout.mergeLayout("layout.ftl",
                FreeMarkerLayout.segment(FreeMarkerLayout.TITLE, "Route list"),
                FreeMarkerLayout.segment(FreeMarkerLayout.CONTENT, body));
    }

    private static String makeRouteHtml(HttpApiDoclet.ApiDoc doc, FreeMarkerLayout layout) {
        String body = layout.merge("route.ftl", new Object[][] {
                {"doc", doc},
        });
        return layout.mergeLayout("layout.ftl",
                FreeMarkerLayout.segment(FreeMarkerLayout.TITLE, "Route " + doc.getRoute()),
                FreeMarkerLayout.segment(FreeMarkerLayout.CONTENT, body));
    }

    private static String makeIndexHtml(HttpApiDoclet.ApiDocs docs, FreeMarkerLayout layout) {
        String frameSet = layout.merge("index.ftl", new Object[][] {
                {"docs", docs},
        });
        return layout.mergeLayout("layout.ftl",
                FreeMarkerLayout.segment(FreeMarkerLayout.TITLE, "Borqs Server Api documents"),
                FreeMarkerLayout.segment(FreeMarkerLayout.CONTENT, frameSet));
    }

    private static String makeAboutHtml(HttpApiDoclet.ApiDocs docs, FreeMarkerLayout layout) {
        String body = layout.merge("about.ftl", new Object[][] {
                {"docs", docs},
        });
        return layout.mergeLayout("layout.ftl",
                FreeMarkerLayout.segment(FreeMarkerLayout.TITLE, "About"),
                FreeMarkerLayout.segment(FreeMarkerLayout.CONTENT, body));
    }

    private static String makeTopHtml(HttpApiDoclet.ApiDocs docs, FreeMarkerLayout layout) {
        String body = layout.merge("top.ftl", new Object[][] {
                {"docs", docs},
        });
        return layout.mergeLayout("layout.ftl",
                FreeMarkerLayout.segment(FreeMarkerLayout.TITLE, "Title"),
                FreeMarkerLayout.segment(FreeMarkerLayout.CONTENT, body));
    }

}
