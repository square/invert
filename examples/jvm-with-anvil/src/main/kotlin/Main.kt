import com.squareup.anvil.annotations.ContributesBinding
import com.squareup.anvil.annotations.ContributesTo
import javax.inject.Inject

fun main() {
    println("hi")
}

abstract class AppScope private constructor()

interface Authenticator

@ContributesBinding(AppScope::class)
class RealAuthenticator @Inject constructor() : Authenticator