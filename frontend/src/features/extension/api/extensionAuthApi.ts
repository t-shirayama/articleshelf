import { request } from '../../../shared/api/client'

export interface ExtensionAuthorizeInput {
  clientId: string
  extensionId: string
  redirectUri: string
  state: string
  codeChallenge: string
  codeChallengeMethod: string
}

interface ExtensionAuthorizeResponse {
  redirectUri: string
  code: string
  state: string
}

export const extensionAuthApi = {
  authorize(input: ExtensionAuthorizeInput): Promise<ExtensionAuthorizeResponse> {
    return request<ExtensionAuthorizeResponse>('/api/extension/oauth/authorize', {
      method: 'POST',
      body: JSON.stringify(input)
    })
  }
}
