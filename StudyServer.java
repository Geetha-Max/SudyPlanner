import java.io.*;
import java.net.*;
import java.util.*;

class Subject {
    String name;
    int time;
    int importance;
    double ratio;

    Subject(String name, int time, int importance) {
        this.name = name;
        this.time = time;
        this.importance = importance;
        this.ratio = (double) importance / time;
    }
}

public class StudyServer {

    public static void main(String[] args) throws Exception {
        ServerSocket server = new ServerSocket(8080);
        System.out.println("Server running at http://localhost:8080");

        while (true) {
            Socket socket = server.accept();

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            OutputStream out = socket.getOutputStream();

            String request = in.readLine();
            if (request == null) continue;

            String query = request.split(" ")[1];

            if (query.contains("?")) {
                query = query.substring(query.indexOf("?") + 1);

                Map<String, String> params = new HashMap<>();
                for (String pair : query.split("&")) {
                    String[] p = pair.split("=");
                    if (p.length == 2)
                        params.put(p[0], URLDecoder.decode(p[1], "UTF-8"));
                }

                int count = 0;
                while (params.containsKey("s" + (count + 1))) count++;

                Subject[] arr = new Subject[count];

                for (int i = 0; i < count; i++) {
                    arr[i] = new Subject(
                            params.get("s" + (i + 1)),
                            Integer.parseInt(params.get("t" + (i + 1))),
                            Integer.parseInt(params.get("i" + (i + 1)))
                    );
                }

                int totalTime = Integer.parseInt(params.get("W"));

                // Sort by ratio
                Arrays.sort(arr, (a, b) -> Double.compare(b.ratio, a.ratio));

                // Store merged durations
                Map<String, Integer> result = new LinkedHashMap<>();

                for (int i = 0; i < count && totalTime > 0; i++) {

                    int studyTime = Math.min(arr[i].time, totalTime);
                    int used = 0;

                    while (used < studyTime) {

                        int block = Math.min(60, studyTime - used);

                        result.put(arr[i].name,
                            result.getOrDefault(arr[i].name, 0) + block);

                        used += block;
                        totalTime -= block;
                    }
                }

                // Build UI
                StringBuilder html = new StringBuilder();

                html.append("<html><head><style>");
                html.append("body { font-family:'Segoe UI'; background:#eef2f7; text-align:center; }");
                html.append(".card { width:60%; margin:40px auto; background:white; padding:20px; border-radius:12px; box-shadow:0 6px 18px rgba(0,0,0,0.15); }");
                html.append("h2 { color:#333; }");
                html.append("table { width:100%; border-collapse:collapse; margin-top:20px; }");
                html.append("th { background:#4CAF50; color:white; padding:12px; }");
                html.append("td { padding:12px; border-bottom:1px solid #ddd; }");
                html.append("tr:nth-child(even) { background:#f9f9f9; }");
                html.append("</style></head><body>");

                html.append("<div class='card'>");
                html.append("<h2>Your Study Schedule</h2>");
                html.append("<table>");
                html.append("<tr><th>Subject</th><th>Duration</th></tr>");

                for (String subject : result.keySet()) {
                    int minutes = result.get(subject);

                    int hrs = minutes / 60;
                    int mins = minutes % 60;

                    String duration = "";
                    if (hrs > 0) duration += hrs + " hr ";
                    if (mins > 0) duration += mins + " min";

                    html.append("<tr><td>")
                        .append(subject)
                        .append("</td><td>")
                        .append(duration.trim())
                        .append("</td></tr>");
                }

                html.append("</table></div></body></html>");

                out.write(("HTTP/1.1 200 OK\r\nContent-Type: text/html\r\n\r\n").getBytes());
                out.write(html.toString().getBytes());
            }

            socket.close();
        }
    }
}