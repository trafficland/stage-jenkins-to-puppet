# /etc/puppet/modules/REPLACE_MODULE_NAME/manifests/params.pp
#class REPLACE_MODULE_NAME::params {

        $REPLACE_MODULE_NAME_version = $::hostname ? {
            default => "1.0.1",
		}
        $REPLACE_MODULE_NAME_base = $::hostname ? {
            default => "/opt",
        }
}
