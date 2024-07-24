package gov.nih.nci.evs.restapi.util;

import java.io.*;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.*;
import java.nio.charset.Charset;

import javax.net.ssl.*;
import java.security.cert.*;
import java.security.*;

public class DownloadPage {

    public static Vector download(String url) {
		Vector w = new Vector();
		try {
			URL urlObj = new URL(url);
			URLConnection urlConnection = urlObj.openConnection();
			Charset charset = Charset.forName("UTF8");
			InputStreamReader stream = new InputStreamReader(urlConnection.getInputStream(), charset);
			BufferedReader reader = new BufferedReader(stream);
			StringBuffer responseBuffer = new StringBuffer();
			String line = null;
			while ((line = reader.readLine()) != null) {
				w.add(line);
			}
			reader.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return w;
	}

	public static void download(String download_url, File file) {
		try {
			byte[] buffer = new byte[1024];
			double TotalDownload = 0.00;
			int readbyte = 0;
			double percentOfDownload = 0.00;

			try {
				doTrustToCertificates();
			} catch (Exception ex) {
				ex.printStackTrace();
			}

			URL url = new URL(download_url);
			HttpURLConnection http = (HttpURLConnection)url.openConnection();
			double filesize = (double)http.getContentLengthLong();

			BufferedInputStream input = new BufferedInputStream(http.getInputStream());
			FileOutputStream ouputfile = new FileOutputStream(file);
			BufferedOutputStream bufferOut = new BufferedOutputStream(ouputfile, 1024);

			while((readbyte = input.read(buffer, 0, 1024)) >= 0) {
				bufferOut.write(buffer,0,readbyte);
				TotalDownload += readbyte;
				percentOfDownload = (TotalDownload*100)/filesize;
				String percent = String.format("%.2f", percentOfDownload);
				System.out.println("Downloaded "+ percent + "%");
			}

			System.out.println("Download is complete.");
			bufferOut.close();
			input.close();
		}
		catch(IOException e){
			e.printStackTrace();
		}
	}

	 // trusting all certificate
	 public static void doTrustToCertificates() throws Exception {
        //Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
        TrustManager[] trustAllCerts = new TrustManager[]{
			new X509TrustManager() {
				public X509Certificate[] getAcceptedIssuers() {
					return null;
				}

				public void checkServerTrusted(X509Certificate[] certs, String authType) throws CertificateException {
					return;
				}

				public void checkClientTrusted(X509Certificate[] certs, String authType) throws CertificateException {
					return;
				}
			}
        };

        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        HostnameVerifier hv = new HostnameVerifier() {
            public boolean verify(String urlHostName, SSLSession session) {
                if (!urlHostName.equalsIgnoreCase(session.getPeerHost())) {
                    System.out.println("Warning: URL host '" + urlHostName + "' is different to SSLSession host '" + session.getPeerHost() + "'.");
                }
                return true;
            }
        };
        HttpsURLConnection.setDefaultHostnameVerifier(hv);
    }

	public static void main(String[] args) {
		String link = "https://www.africau.edu/images/default/sample.pdf";
		File file = new File("sample.pdf");
		download(link, file);
	}
}
