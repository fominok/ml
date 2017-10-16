import numpy as np
import pandas as pd
from sklearn.naive_bayes import GaussianNB
from sklearn.neighbors import KNeighborsClassifier

def prepare_ds(name):
    ds = pd.read_csv(name)
    ds_attrs = ds.drop(['cov_type'], axis=1).values
    ds_class = ds['cov_type'].values
    return ds_attrs, ds_class

ds_train_attrs, ds_train_class = prepare_ds('cov_train_r.csv')
ds_test_attrs, ds_test_class = prepare_ds('cov_test_r.csv')

clf_a = GaussianNB()
clf_a.fit(ds_train_attrs, ds_train_class)
print(clf_a.score(ds_test_attrs, ds_test_class))
# >> 46.3487829276

clf_b = KNeighborsClassifier()
clf_b.fit(ds_train_attrs, ds_train_class)
print(clf_b.score(ds_test_attrs, ds_test_class))
# >> 69.9233077693
