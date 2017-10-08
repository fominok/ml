import numpy as np
import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.naive_bayes import GaussianNB

ds = pd.read_csv('covtype_data.csv', header=None).values

attrs = ds[:, 0:-1]
classes = ds[:, -1]
classes = classes.astype(np.int64, copy=False)
data_train, data_test, class_train, class_test = train_test_split(
    attrs, classes, test_size=0.3, random_state=55)

clf = GaussianNB()
clf.fit(data_train, class_train)
print(clf.score(data_test, class_test))
# >> 0.45619721865246926
