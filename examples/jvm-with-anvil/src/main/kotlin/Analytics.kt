import com.squareup.anvil.annotations.ContributesBinding
import com.squareup.anvil.annotations.ContributesTo
import javax.inject.Inject

interface Analytics

@ContributesBinding(AppScope::class)
class RealAnalytics @Inject constructor() : Analytics