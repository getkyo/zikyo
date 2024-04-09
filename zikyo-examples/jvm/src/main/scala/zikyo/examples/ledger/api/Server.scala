package zikyo.examples.ledger.api

import java.util.concurrent.Executors
import kyo.*
import scala.concurrent.duration.*
import sttp.tapir.server.netty.*
import zikyo.*
import zikyo.examples.ledger.db.DB

object Server extends KyoApp:

    def flag(name: String, default: String) =
        Option(System.getenv(name))
            .getOrElse(System.getProperty(name, default))

    run {
        val port = flag("PORT", "9999").toInt

        val dbConfig =
            DB.Config(
                flag("DB_PATH", "/tmp/"),
                flag("flushInternalMs", "1000").toInt.millis
            )

        val options =
            NettyKyoServerOptions
                .default(enableLogging = false)
                .forkExecution(false)

        val cfg =
            NettyConfig.default
                .withSocketKeepAlive
                .copy(lingerTimeout = None)

        val server =
            NettyKyoServer(options, cfg)
                .host("0.0.0.0")
                .port(port)

        val timer = Timer(Executors.newSingleThreadScheduledExecutor())

        val init = Endpoints.init
            .provide(Handler.init)
            .provideAs[DB](DB.init)
            .provide(dbConfig)

        defer {
            await(Consoles.println(s"Server starting on port $port..."))
            val binding = await(Routes.run(server)(Timers.let(timer)(init)))
            await(Consoles.println(s"Server started: ${binding.localSocket}"))
        }
    }

end Server
