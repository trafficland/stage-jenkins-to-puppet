package actors.http.view

import spray.http.{MediaTypes, HttpBody, StatusCodes, HttpResponse}

trait IScheduledViews {
  def routesView: HttpResponse = {
    HttpResponse.apply(status = StatusCodes.Accepted,
      entity = HttpBody(MediaTypes.`text/html`,
        <html>
          <body>
            <h1>Actor Hook hit
              <i>spray-can</i>
              !</h1>
            <p>Defined resources:</p>
            <ul>
              <li>
                <a href="/actorHook/routes">/actorHook/routes</a>
              </li>
              <li>
                <a href="/actorHook/scheduled">/actorHook/scheduled</a>
              </li>
            </ul>
          </body>
        </html>.toString))
  }

  def hookView: HttpResponse =
    HttpResponse.apply(status = StatusCodes.Accepted,
      entity = HttpBody(MediaTypes.`text/html`,
        <html>
          <body>
            <h1>Actor Hook hit from
              <i>spray-can</i>
              !</h1>
          </body>
        </html>.toString
      ))

  def pollOnOff(onOffString: String = "off"): HttpResponse =
    HttpResponse.apply(status = StatusCodes.Accepted,
      entity = HttpBody(MediaTypes.`text/plain`, "polling is " + onOffString + '!'))
}


trait IBasicViews {
  def notFoundView = HttpResponse.apply(status = StatusCodes.Accepted,
    entity = HttpBody(MediaTypes.`text/html`,
      <html>
        <body>
          <h1>Actor Hook hit
            <i>spray-can</i>
            !</h1>
          <p>Resource Not FOUND!!</p>
        </body>
      </html>.toString
    ))
}