package com

import scopes.AppScope
import com.squareup.anvil.annotations.ContributesBinding
import javax.inject.Inject

interface Authenticator

@ContributesBinding(AppScope::class)
class RealAuthenticator @Inject constructor(
    private val analytics: Analytics,
) : Authenticator
