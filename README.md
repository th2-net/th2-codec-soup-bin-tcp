# SoupBinTCP Codec v0.0.2

## Configuration

This codec does not require configuration

## Protocol

This codec works with messages of `SOUP_BIN_TCP` protocol

## Inputs/outputs

This section describes messages received and by produced by the service

### Encode input

* parsed header message and raw payload message
* raw payload message (in this case it will be encoded as unsequenced data packet)

### Encode output / Decode input

* raw message containing header and payload

### Decode output

* parsed header message and raw payload message

## Deployment via `infra-mgr`

Here's an example of custom resource that is required for [infra-mgr](https://github.com/th2-net/th2-infra-mgr) to deploy this component.
_Custom resource_ for kubernetes is a common way to deploy components in th2.
You can find more information about th2 components deployment in the [th2 wiki](https://github.com/th2-net/th2-documentation/wiki).

```yaml
apiVersion: th2.exactpro.com/v1
kind: Th2Box
metadata:
  name: codec-soup-bin-tcp
spec:
  image-name: ghcr.io/th2-net/th2-codec-soup-bin-tcp
  image-version: 0.0.1
  custom-config:
    codecSettings: { }
  type: th2-codec
  pins:
    # encoder
    - name: in_codec_encode
      connection-type: mq
      attributes:
        - encoder_in
        - subscribe
    - name: out_codec_encode
      connection-type: mq
      attributes:
        - encoder_out
        - publish
    # decoder
    - name: in_codec_decode
      connection-type: mq
      attributes:
        - decoder_in
        - subscribe
    - name: out_codec_decode
      connection-type: mq
      attributes:
        - decoder_out
        - publish
    # encoder general (technical)
    - name: in_codec_general_encode
      connection-type: mq
      attributes:
        - general_encoder_in
        - subscribe
    - name: out_codec_general_encode
      connection-type: mq
      attributes:
        - general_encoder_out
        - publish
    # decoder general (technical)
    - name: in_codec_general_decode
      connection-type: mq
      attributes:
        - general_decoder_in
        - subscribe
    - name: out_codec_general_decode
      connection-type: mq
      attributes:
        - general_decoder_out
        - publish
  extended-settings:
    service:
      enabled: false
```

# Release notes
## 0.0.2
+ th2-common -> `3.44.0`
+ th2-bom -> `4.1.0`
+ th2-codec -> `4.7.6`
+ Vulnerability check pipeline step