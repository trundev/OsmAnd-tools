package net.osmand.server.controllers.pub;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.xmlpull.v1.XmlPullParserException;

import com.google.gson.Gson;

import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.GPXTrackAnalysis;
import net.osmand.IndexConstants;
import net.osmand.server.controllers.pub.UserSessionResources.GPXSessionContext;
import net.osmand.obf.OsmGpxWriteContext;
import net.osmand.util.Algorithms;

import static net.osmand.obf.OsmGpxWriteContext.*;


@RestController
@RequestMapping("/gpx/")
public class GpxController {
	

	Gson gson = new Gson();
	
	@Autowired
	UserSessionResources session;
	
	public static class GPXSessionInfo {
		public double totalDist;
		public int trkPoints;
		public int waypoints;
		public int files;
		
		public static GPXSessionInfo getInfo(GPXSessionContext c) {
			GPXSessionInfo info = new GPXSessionInfo();
			info.files = c.files.size();
			for(int i = 0; i < c.files.size(); i++ ) {
				GPXTrackAnalysis analysis = c.analysis.get(i);
				info.totalDist += analysis.totalDistance;
				info.trkPoints += analysis.points;
				info.waypoints += analysis.wptPoints;
			}
			return info;
		}
	}
	
	
	
	@PostMapping(path = {"/clear"}, produces = "application/json")
	@ResponseBody
	public String clear(HttpServletRequest request, HttpSession httpSession) throws IOException {
		GPXSessionContext ctx = session.getGpxResources(httpSession);
		for (File f : ctx.tempFiles) {
			f.delete();
		}
		ctx.tempFiles.clear();
		ctx.files.clear();
		ctx.analysis.clear();
		return gson.toJson(GPXSessionInfo.getInfo(ctx));
	}
	
	@GetMapping(path = {"/get-gpx-info"}, produces = "application/json")
	@ResponseBody
	public String getGpx(HttpServletRequest request, HttpSession httpSession) throws IOException {
		GPXSessionContext ctx = session.getGpxResources(httpSession);
		return gson.toJson(GPXSessionInfo.getInfo(ctx));
	}
	
	@PostMapping(path = {"/upload-session-gpx"}, produces = "application/json")
	@ResponseBody
	public String uploadGpx(HttpServletRequest request, HttpSession httpSession,
			@RequestParam(required = true) MultipartFile file) throws IOException {
		GPXSessionContext ctx = session.getGpxResources(httpSession);
		File tmpGpx = File.createTempFile("gpx_" + httpSession.getId(), ".gpx");
		ctx.tempFiles.add(tmpGpx);
		InputStream is = file.getInputStream();
		FileOutputStream fous = new FileOutputStream(tmpGpx);
		Algorithms.streamCopy(is, fous);
		is.close();
		fous.close();
		GPXFile gpxFile = GPXUtilities.loadGPXFile(tmpGpx);
		if (gpxFile != null) {
			GPXTrackAnalysis analysis = gpxFile.getAnalysis(System.currentTimeMillis());
			ctx.files.add(tmpGpx);
			ctx.analysis.add(analysis);
		}
		return gson.toJson(GPXSessionInfo.getInfo(ctx));
	}
	

	
	
	@RequestMapping(path = { "/download-obf"})
	@ResponseBody
    public ResponseEntity<Resource> downloadObf(@RequestParam(defaultValue="", required=false) String gzip,
			HttpSession httpSession, HttpServletResponse resp) throws IOException, FactoryConfigurationError,
			XMLStreamException, SQLException, InterruptedException, XmlPullParserException {
		GPXSessionContext ctx = session.getGpxResources(httpSession);
		File tmpOsm = File.createTempFile("gpx_obf_" + httpSession.getId(), ".osm.gz");
		ctx.tempFiles.add(tmpOsm);
		List<File> files = ctx.files;
		String sessionId = httpSession.getId();
		File tmpFolder = new File(tmpOsm.getParentFile(), sessionId);
		String fileName = "gpx_" + sessionId;
		QueryParams qp = new QueryParams();
		qp.osmFile = tmpOsm;
		qp.details = QueryParams.DETAILS_ELE_SPEED;
		OsmGpxWriteContext writeCtx = new OsmGpxWriteContext(qp);
		File targetObf = new File(tmpFolder.getParentFile(), fileName + IndexConstants.BINARY_MAP_INDEX_EXT);
		writeCtx.writeObf(files, tmpFolder, fileName, targetObf);
		ctx.tempFiles.add(targetObf);
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.CONTENT_DISPOSITION, String.format("attachment; filename=\"gpx.obf\""));
		headers.add(HttpHeaders.CONTENT_TYPE, "application/octet-binary");
		return ResponseEntity.ok().headers(headers).body(new FileSystemResource(targetObf));
	}


}
