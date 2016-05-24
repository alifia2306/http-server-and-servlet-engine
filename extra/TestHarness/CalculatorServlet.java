package edu.upenn.cis.cis455;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class CalculatorServlet extends HttpServlet {
  public void doGet(HttpServletRequest request, HttpServletResponse response) 
       throws java.io.IOException
  {
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();
    int v1 = Integer.valueOf(request.getParameter("num1")).intValue();
    int v2 = Integer.valueOf(request.getParameter("num2")).intValue();
    out.println("<html><head><title>Foo</title></head>");
    out.println("<body>"+v1+"+"+v2+"="+(v1+v2)+"</body></html>");
  }
}
  
