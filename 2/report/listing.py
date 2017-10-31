import numpy as np
import pandas as pd
from sklearn.tree import DecisionTreeClassifier
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import train_test_split

def prepare_ds(name):
    ds = pd.read_csv(name)
    ds_attrs = ds.drop(['cov_type'], axis=1).values
    ds_class = ds['cov_type'].values
    return ds_attrs, ds_class

def main():
    attrs, classes = prepare_ds('cov_data.csv')
    for s in np.arange(0.1, 0.4, 0.1):
        data_train, data_test, class_train, class_test = train_test_split(
                attrs,
                classes,
                test_size=s,
                random_state=54,
        )
        decision_tree = DecisionTreeClassifier()
        decision_tree.fit(data_train, class_train)
        decision_tree_score = decision_tree.score(data_test, class_test)
        print("Decision tree size: {0:.1f}; Accuracy: {1:.3f}".format(s, decision_tree_score))

        random_forest = RandomForestClassifier()
        random_forest.fit(data_train, class_train)
        random_forest_score = random_forest.score(data_test, class_test)
        print("Random forest size: {0:.1f}; Accuracy: {1:.3f}".format(s, random_forest_score))

if __name__ == '__main__':
    main()

# Decision tree size: 0.1; Accuracy: 0.718
# Random forest size: 0.1; Accuracy: 0.775
# Decision tree size: 0.2; Accuracy: 0.709
# Random forest size: 0.2; Accuracy: 0.771
# Decision tree size: 0.3; Accuracy: 0.701
# Random forest size: 0.3; Accuracy: 0.758
# Decision tree size: 0.4; Accuracy: 0.700
# Random forest size: 0.4; Accuracy: 0.757
