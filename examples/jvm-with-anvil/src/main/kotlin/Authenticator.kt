import com.squareup.anvil.annotations.ContributesBinding
import com.squareup.anvil.annotations.ContributesTo
import javax.inject.Inject

interface Authenticator

@ContributesBinding(AppScope::class)
class RealAuthenticator @Inject constructor() : Authenticator
