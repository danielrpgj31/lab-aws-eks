
# Kube-prometheus-stack 

https://github.com/prometheus-operator/kube-prometheus
drjunior_br@LIBRA:~/dev/src$ helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update
helm install obs-stack prometheus-community/kube-prometheus-stack
"prometheus-community" has been added to your repositories
Hang tight while we grab the latest from your chart repositories...
...Successfully got an update from the "prometheus-community" chart repository
Update Complete. ⎈Happy Helming!⎈
NAME: obs-stack
LAST DEPLOYED: Tue Apr 21 09:19:56 2026
NAMESPACE: default
STATUS: deployed
REVISION: 1
TEST SUITE: None
NOTES:
kube-prometheus-stack has been installed. Check its status by running:
  kubectl --namespace default get pods -l "release=obs-stack"

Get Grafana 'admin' user password by running:

  kubectl --namespace default get secrets obs-stack-grafana -o jsonpath="{.data.admin-password}" | base64 -d ; echo

Access Grafana local instance:

  export POD_NAME=$(kubectl --namespace default get pod -l "app.kubernetes.io/name=grafana,app.kubernetes.io/instance=obs-stack" -oname)
  kubectl --namespace default port-forward $POD_NAME 3000

Get your grafana admin user password by running:

  kubectl get secret --namespace default -l app.kubernetes.io/component=admin-secret -o jsonpath="{.items[0].data.admin-password}" | base64 --decode ; echo


Visit https://github.com/prometheus-operator/kube-prometheus for instructions on how to create & configure Alertmanager and Prometheus instances using the Operator.
