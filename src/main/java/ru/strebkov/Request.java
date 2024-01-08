package ru.strebkov;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.RequestContext;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;

import java.net.URI;
import java.net.URISyntaxException;

import java.nio.charset.StandardCharsets;
import java.util.*;


public class Request {
    private final String method;
    private final String path;
    public static final String GET = "GET";
    public static final String POST = "POST";
    private final List<String> headers;
    private final List<NameValuePair> quaryparams;
    private static int markAndByteLimit = 4096;

    private static String bodyPost;

    public Request(String method, String path, List<String> headers, List<NameValuePair> quaryparams) {
        this.method = method;
        this.path = path;
        this.headers = headers;
        this.quaryparams = quaryparams;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public List<NameValuePair> getQuaryParams() {
        return quaryparams;
    }


    public NameValuePair getQueryParam(String name) {
        return getQuaryParams().stream()
                .filter(param -> param.getName().equalsIgnoreCase(name))
                .findFirst().orElse(new NameValuePair() {
                    @Override
                    public String getName() {
                        return null;
                    }

                    @Override
                    public String getValue() {
                        return null;
                    }
                });
    }

   static Request createRequest(BufferedInputStream in, BufferedOutputStream out) throws IOException, URISyntaxException {
        final List<String> allowedMethods = List.of(GET, POST);
        in.mark(markAndByteLimit);
        final var buffer = new byte[markAndByteLimit];
        final var read = in.read(buffer);
        // ищем request line
        final var requestLineDelimiter = new byte[]{'\r', '\n'};
        final var requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);
        if (requestLineEnd == -1) {
            badRequest(out);
            return null;
        }
        // читаем request line
        final var requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");
        if (requestLine.length != 3) {
            badRequest(out);
            return null;
        }

        final var method = requestLine[0];
        if (!allowedMethods.contains(method)) {
            badRequest(out);
            return null;
        }
        System.out.println(method);

        final var path = requestLine[1];
        if (!path.startsWith("/")) {
            badRequest(out);
            return null;
        }
        System.out.println(path);

        // ищем заголовки
        final var headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
        final var headersStart = requestLineEnd + requestLineDelimiter.length;
        final var headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);
        if (headersEnd == -1) {
            badRequest(out);
            return null;
        }
        // отматываем на начало буфера
        in.reset();
        // пропускаем requestLine
        in.skip(headersStart);

        final var headersBytes = in.readNBytes(headersEnd - headersStart);
        List<String> headers = Arrays.asList(new String(headersBytes).split("\r\n"));
        System.out.println(headers);
        List<NameValuePair> params = URLEncodedUtils.parse(new URI(path), String.valueOf(StandardCharsets.UTF_8));

        bodyPost = getPostBody(in, method, headersDelimiter, headers);

        return new Request(method, path, headers, params);
    }

    // from google guava with modifications
    private static int indexOf(byte[] array, byte[] target, int start, int max) {
        outer:
        for (int i = start; i < max - target.length + 1; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    private static void badRequest(BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 400 Bad Request\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }

    public static String getPostBody(BufferedInputStream in, String method, byte[] headersDelimiter, List<String> headers) throws IOException {
        if (!method.equals(GET)) {
            in.skip(headersDelimiter.length);
            // вычитываем Content-Length, чтобы прочитать body
            final var contentLength = extractHeader(headers, "Content-Length");
            final var contentType = extractHeader(headers, "Content-Type");
            if (contentLength.isPresent() && contentType.equals("application/x-www-form-urlencoded")) {
                final var length = Integer.parseInt(contentLength.get());
                final byte[] bodyBytes;
                bodyBytes = in.readNBytes(length);
                final var body = new String(bodyBytes);
                System.out.println(body);
                return body;
            }
            if (contentType.equals("multipart/form-data")){
                getParts();
            }
        }
        return null;
    }

    private static Optional<String> extractHeader(List<String> headers, String header) {
        return headers.stream()
                .filter(o -> o.startsWith(header))
                .map(o -> o.substring(o.indexOf(" ")))
                .map(String::trim)
                .findFirst();
    }

    public static List<NameValuePair> getPostParams() {
        List<NameValuePair> bodyList = null;
        try {
            bodyList = URLEncodedUtils.parse(new URI(bodyPost), String.valueOf(StandardCharsets.UTF_8));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        return bodyList;
    }

    public static NameValuePair getPostParam(String name) {
        return getPostParams().stream()
                .filter(param -> param.getName().equalsIgnoreCase(name))
                .findFirst().orElse(new NameValuePair() {
                    @Override
                    public String getName() {
                        return null;
                    }

                    @Override
                    public String getValue() {
                        return null;
                    }
                });
    }

    public static List<FileItem> getParts() {
        DiskFileItemFactory diskFileIF = new DiskFileItemFactory();
        diskFileIF.setRepository(new File("c:/a"));
        diskFileIF.setSizeThreshold(1024 * 8);

        ServletFileUpload upload = new ServletFileUpload(diskFileIF);
        upload.setHeaderEncoding("UTF-8");
        upload.setFileSizeMax(1024 * 1024 * 5);
        upload.setSizeMax(1024 * 1024 * 10);

        Request request = (Request) getPostParams();

        List<FileItem> list = null;
        try {
            list = upload.parseRequest((RequestContext) request);// parse
        } catch (FileUploadException e) {
            e.printStackTrace();
        }
        return list;
    }

    public  static List<String> getPart(String name) {
        List<String> listFile = new ArrayList<>();
        String fileName;

        try {
            for (FileItem fileItem : getParts()) {
                if (fileItem.isFormField()) {
                    String onlyText = fileItem.getString("UTF-8");
                    System.err.println("Description is:" + onlyText);
                } else {
                    fileName = fileItem.getName();
                    fileName = fileName.substring(fileName.lastIndexOf("\\") + 1);//Parse file name
                    if (fileName.equals(name)) {
                        listFile.add(fileName);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return listFile;
    }
}
