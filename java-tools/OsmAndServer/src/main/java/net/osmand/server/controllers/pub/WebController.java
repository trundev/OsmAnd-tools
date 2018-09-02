package net.osmand.server.controllers.pub;

import java.io.File;
import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class WebController {
    private static final Log LOGGER = LogFactory.getLog(WebController.class);

    @Value("${web.location}")
    private String websiteLocation;

    // TOP LEVEL API (redirects and static files) 
    @RequestMapping(path = { "tile_sources.php", "tile_sources.xml", "tile_sources"}, produces = {"application/xml"})
	@ResponseBody
    public FileSystemResource tileSourcesXml(@RequestParam(required=false) boolean update, 
    		@RequestParam(required=false) boolean refresh) throws IOException {
        return new FileSystemResource(new File(websiteLocation, "tile_sources.xml")); 
    }
    
    @RequestMapping(path = { "go" })
    public void webLocation(HttpServletResponse response, HttpServletRequest request) {
        response.setHeader("Location", "go.html?" + request.getQueryString());
        response.setStatus(302); 
    }
    
    @RequestMapping(path = { "travel" })
    public void travel(HttpServletResponse response, @RequestParam(required=false) String title, 
    		@RequestParam(required=false) String lang) {
        response.setHeader("Location",  "https://"+lang+".wikivoyage.org/wiki/"+title);
        response.setStatus(302); 
    }
    
    // WEBSITE
    @RequestMapping(path = { "/apps", "/apps.html" })
    public String apps(HttpServletResponse response) {
    	// TODO generate static 
        return "pub/apps.html"; 
    }
    
    @RequestMapping(path = { "/", "/index.html", "/index" })
    public String index(HttpServletResponse response) {
    	// TODO generate static 
        return "pub/index.html"; 
    }
    
    @RequestMapping(path = { "/build_it", "/build_it.html" })
    public String buildIt(HttpServletResponse response) {
    	// TODO generate static 
        return "pub/build_it.html"; 
    }
    @RequestMapping(path = { "/dvr", "/dvr.html"  })
    public String dvr(HttpServletResponse response) {
    	// TODO generate static 
        return "pub/dvr.html"; 
    }
    @RequestMapping(path = { "/osm_live", "/osm_live.html"  })
    public String features(HttpServletResponse response) {
    	// TODO generate static 
        return "pub/osm_live.html"; 
    }
}