
### Example deploy command
```
helm --namespace offender-case-notes-dev  --tiller-namespace offender-case-notes-dev upgrade offender-case-notes ./offender-case-notes/ --install --values=values-dev.yaml --values=example-secrets.yaml
```

### Helm init

```
helm init --tiller-namespace offender-case-notes-dev --service-account tiller --history-max 200
```

### Setup Lets Encrypt cert

```
kubectl -n offender-case-notes-dev apply -f certificate-dev.yaml
kubectl -n offender-case-notes-dev apply -f certificate-preprod.yaml
kubectl -n offender-case-notes-dev apply -f certificate-prod.yaml
```
